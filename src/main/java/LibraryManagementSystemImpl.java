import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try {
            //判断库里面是否已经有这本书
            String sql = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
            PreparedStatement stmt1 = conn.prepareStatement(sql);
            PreparedStatement stmt2 = null;
            stmt1.setString(1, book.getCategory());
            stmt1.setString(2, book.getTitle());
            stmt1.setString(3, book.getPress());
            stmt1.setInt(4, book.getPublishYear());
            stmt1.setString(5, book.getAuthor());
            ResultSet rs = stmt1.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Book already exists");
            } else {
                //插入新书
                stmt2 = conn.prepareStatement("INSERT INTO book (category,title,press,publish_year,author,price,stock) VALUES (?, ?, ?, ?, ?, ?, ?)");
                stmt2.setString(1, book.getCategory());
                stmt2.setString(2, book.getTitle());
                stmt2.setString(3, book.getPress());
                stmt2.setInt(4, book.getPublishYear());
                stmt2.setString(5, book.getAuthor());
                stmt2.setDouble(6, book.getPrice());
                stmt2.setInt(7, book.getStock());
                stmt2.executeUpdate();
            }
            rs = stmt1.executeQuery();
            if (rs.next()) {
                book.setBookId(rs.getInt("book_id"));
            } else {
                return new ApiResult(false, "Failed to get book id");
            }
            commit(conn);
            // stmt1.close();
            // if (stmt2 != null) {
            //     stmt2.close();
            // }
            return new ApiResult(true, book.getBookId());
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        } //finally {
        //     conn.close();
        // }
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        try {
            String sql = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement stmt1 = conn.prepareStatement(sql);
            stmt1.setInt(1, bookId);
            ResultSet rs = stmt1.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Book not found");
            }
            int stock = rs.getInt("stock");
            if (stock + deltaStock < 0) {
                return new ApiResult(false, "Stock can't be negative");
            }
            PreparedStatement stmt2 = conn.prepareStatement("UPDATE book SET stock = ? WHERE book_id = ?");
            stmt2.setInt(1, stock + deltaStock);
            stmt2.setInt(2, bookId);
            stmt2.executeUpdate();
            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try {
            String query_sql = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
            String insert_sql = "INSERT INTO book (category,title,press,publish_year,author,price,stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement query_stmt = conn.prepareStatement(query_sql);
            PreparedStatement insert_stmt = conn.prepareStatement(insert_sql);
            for (Book book : books) {
                query_stmt.setString(1, book.getCategory());
                query_stmt.setString(2, book.getTitle());
                query_stmt.setString(3, book.getPress());
                query_stmt.setInt(4, book.getPublishYear());
                query_stmt.setString(5, book.getAuthor());
                ResultSet rs = query_stmt.executeQuery();
                if (rs.next()) {//说明有这本书了
                    rollback(conn);
                    return new ApiResult(false, "Some Book already exists");
                }
                insert_stmt.setString(1, book.getCategory());
                insert_stmt.setString(2, book.getTitle());
                insert_stmt.setString(3, book.getPress());
                insert_stmt.setInt(4, book.getPublishYear());
                insert_stmt.setString(5, book.getAuthor());
                insert_stmt.setDouble(6, book.getPrice());
                insert_stmt.setInt(7, book.getStock());
                insert_stmt.addBatch();
            }
            insert_stmt.executeBatch();
            for (Book book : books) {
                query_stmt.setString(1, book.getCategory());
                query_stmt.setString(2, book.getTitle());
                query_stmt.setString(3, book.getPress());
                query_stmt.setInt(4, book.getPublishYear());
                query_stmt.setString(5, book.getAuthor());
                ResultSet rs = query_stmt.executeQuery();
                if (rs.next()) {
                    book.setBookId(rs.getInt("book_id"));
                } else {
                    rollback(conn);
                    return new ApiResult(false, "Failed to get book id");
                }
            }
            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        try {
            String query_sql = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement query_stmt = conn.prepareStatement(query_sql);
            query_stmt.setInt(1, bookId);
            ResultSet rs = query_stmt.executeQuery();
            if (!rs.next()) {
                return new ApiResult(false, "Book not found");
            }
            query_sql = "SELECT * FROM borrow WHERE book_id = ? AND return_time = 0";
            query_stmt = conn.prepareStatement(query_sql);
            query_stmt.setInt(1, bookId);
            rs = query_stmt.executeQuery();
            if (rs.next()) {
                return new ApiResult(false, "Book is borrowed");
            }
            PreparedStatement remove_stmt = conn.prepareStatement("DELETE FROM book WHERE book_id = ?");
            remove_stmt.setInt(1, bookId);
            remove_stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        List<Book> books = new ArrayList<>();
        try {
            String sql = "SELECT * FROM book WHERE "
            + "title like ? AND press like ? AND publish_year >= ? AND publish_year <= ? AND author like ? AND price >= ? AND price <= ? AND category like ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            if (conditions.getCategory() != null) {
                stmt.setString(8, "%"+conditions.getCategory()+"%");
            } else {
                stmt.setString(8, "%");
            }
            if (conditions.getTitle() != null) {
                stmt.setString(1, "%"+conditions.getTitle()+"%");
            } else {
                stmt.setString(1, "%");
            }
            if (conditions.getPress() != null) {
                stmt.setString(2, "%"+conditions.getPress()+"%");
            } else {
                stmt.setString(2, "%");
            }
            if (conditions.getMinPublishYear() != null) {
                stmt.setInt(3, conditions.getMinPublishYear());
            } else {
                stmt.setInt(3, -999);
            }
            if (conditions.getMaxPublishYear() != null) {
                stmt.setInt(4, conditions.getMaxPublishYear());
            } else {
                stmt.setInt(4, 9999);
            }
            if (conditions.getAuthor() != null) {
                stmt.setString(5, "%"+conditions.getAuthor()+"%");
            } else {
                stmt.setString(5, "%");
            }
            if (conditions.getMinPrice() != null) {
                stmt.setDouble(6, conditions.getMinPrice());
            } else {
                stmt.setDouble(6, -999999999);
            }
            if (conditions.getMaxPrice() != null) {
                stmt.setDouble(7, conditions.getMaxPrice());
            } else {
                stmt.setDouble(7, 999999999);
            }
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Book book = new Book(rs.getString("category"),
                 rs.getString("title"), rs.getString("press"), 
                 rs.getInt("publish_year"), rs.getString("author"), rs.getDouble("price"), rs.getInt("stock"));
                book.setBookId(rs.getInt("book_id"));
                books.add(book);
            }
            if (conditions.getSortOrder() == SortOrder.ASC) {
                books.sort(conditions.getSortBy().getComparator());
            } else {
                books.sort(conditions.getSortBy().getComparator().reversed());
            }
            stmt.close();
            commit(conn);           
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        BookQueryResults results = new BookQueryResults(books);
        return new ApiResult(true, results);
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            String sql = "SELECT * FROM book WHERE book_id = ?";
            PreparedStatement query_stmt = conn.prepareStatement(sql);
            query_stmt.setInt(1, borrow.getBookId());
            ResultSet borrow_book = query_stmt.executeQuery();
            if (!borrow_book.next()) {//说明没有这本书
                return new ApiResult(false, "Book not found");
            }
            else if (borrow_book.getInt("stock") == 0) {//说明没有库存
                return new ApiResult(false, "No stock");
            }
            else {
                //查找符合条件的并且没有归还的书籍
                String query_borrow_sql = "SELECT * FROM borrow WHERE book_id = ? AND card_id = ? AND return_time = 0";
                query_stmt = conn.prepareStatement(query_borrow_sql);
                query_stmt.setInt(1, borrow.getBookId());
                query_stmt.setInt(2, borrow.getCardId());
                ResultSet record = query_stmt.executeQuery();
                if (!record.next()) {//此人没借过这本书或者已经还了
                    //更新了插入时不用赋值return_time
                    String borrow_sql = "INSERT INTO borrow (card_id, book_id, borrow_time) VALUES(?,?,?)";
                    String de_stock_sql = "UPDATE book SET stock = ? WHERE book_id = ?";
                    PreparedStatement exuStatement = conn.prepareStatement(borrow_sql);
                    exuStatement.setInt(1, borrow.getCardId());
                    exuStatement.setInt(2, borrow.getBookId());
                    exuStatement.setLong(3, borrow.getBorrowTime());
                    exuStatement.executeUpdate();
                    PreparedStatement de_stock_stmt = conn.prepareStatement(de_stock_sql);
                    de_stock_stmt.setInt(1, borrow_book.getInt("stock")-1);
                    de_stock_stmt.setString(2, borrow_book.getString("book_id"));
                    de_stock_stmt.executeUpdate();
                } else {
                    rollback(conn);
                    return new ApiResult(false, "You had borrowed this book");
                }
            }
            commit(conn);
            return new ApiResult(true, null);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            if (borrow.getReturnTime() <= borrow.getBorrowTime()) {
                return new ApiResult(false, "Invalid return time");
            }
            String update_sql = "UPDATE borrow SET return_time = ? WHERE book_id = ? AND card_id = ? AND borrow_time = ? AND return_time = 0";
            String inc_sql = "UPDATE book SET stock = stock+1 WHERE book_id = ?";
            PreparedStatement up_stmt = conn.prepareStatement(update_sql);
            PreparedStatement inc_stmt = conn.prepareStatement(inc_sql);
            up_stmt.setLong(1, borrow.getReturnTime());
            up_stmt.setInt(2, borrow.getBookId());
            up_stmt.setInt(3, borrow.getCardId());
            up_stmt.setLong(4, borrow.getBorrowTime());
            int res = up_stmt.executeUpdate();
            if (res == 0) {//如果没有这条借书记录
                rollback(conn);
                return new ApiResult(false, "No such borrow record");
            }
            inc_stmt.setInt(1, borrow.getBookId());
            inc_stmt.executeUpdate();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        List<BorrowHistories.Item> histories = new ArrayList<>();
        try {
            String query_sql = "SELECT * FROM borrow WHERE card_id = ? ORDER BY borrow_time DESC, book_id ASC";
            PreparedStatement query_stmt = conn.prepareStatement(query_sql);
            query_stmt.setInt(1, cardId);
            ResultSet records = query_stmt.executeQuery();
            while (records.next()) {//遍历所有的借书记录
                String book_sql = "SELECT * FROM book WHERE book_id = ?";//获取本书的信息
                PreparedStatement book_stmt = conn.prepareStatement(book_sql);
                book_stmt.setInt(1, records.getInt("book_id"));
                ResultSet book = book_stmt.executeQuery();
                if (book.next()) {//如果查得到这本书
                    Book book1 = new Book(book.getString("category"), 
                        book.getString("title"), book.getString("press"), 
                        book.getInt("publish_year"), book.getString("author"), 
                        book.getDouble("price"), book.getInt("stock"));
                    book1.setBookId(book.getInt("book_id"));
                    Borrow borrow1 = new Borrow(cardId, book1.getBookId());   
                    borrow1.setBorrowTime(records.getLong("borrow_time"));
                    borrow1.setReturnTime(records.getLong("return_time"));
                    BorrowHistories.Item item = new BorrowHistories.Item(cardId, book1, borrow1);
                    histories.add(item);
                }
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        BorrowHistories results = new BorrowHistories(histories);
        return new ApiResult(true, results);
    }

    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            String query_sql = "SELECT * FROM card WHERE name = ? AND department = ? AND type = ?";
            String insert_sql = "INSERT INTO card (name, department, type) VALUES (?, ? ,?)";
            PreparedStatement query_stmt = conn.prepareStatement(query_sql);
            PreparedStatement insert_stmt = conn.prepareStatement(insert_sql);
            query_stmt.setString(1, card.getName());
            query_stmt.setString(2, card.getDepartment());
            query_stmt.setString(3, card.getType().getStr());
            ResultSet rs_card = query_stmt.executeQuery();
            if (rs_card.next()) {//如果有查询到的卡，回退
                rollback(conn);
                return new ApiResult(false, "Card alrady exits");
            } else {
                insert_stmt.setString(1, card.getName());
                insert_stmt.setString(2, card.getDepartment());
                insert_stmt.setString(3, card.getType().getStr());
                insert_stmt.executeUpdate();
                rs_card = query_stmt.executeQuery();
                if (rs_card.next()) {
                    card.setCardId(rs_card.getInt("card_id"));
                }
            }
            query_stmt.close();
            insert_stmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    @Override
    public ApiResult removeCard(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showCards() {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
