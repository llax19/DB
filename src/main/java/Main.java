import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;
import com.sun.net.httpserver.HttpServer;

import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import queries.BorrowHistories;
import queries.CardList;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());
    private static DatabaseConnector connector;
    private static LibraryManagementSystem library;

    private static ConnectConfig connectConfig = null;

    static {
        try {
            // parse connection config from "resources/application.yaml"
            connectConfig = new ConnectConfig();
            log.info("Success to parse connect config. " + connectConfig.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //从网上找来的一个解析query的方法
    static Map<String, String> queryToMap(String query) {
        if(query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }
    
    static Map<String, String> PostToMap(String query_old) {
        if(query_old == null) {
            return null;
        }
        //去除两边的冗余字段,即大括号
        String query = query_old.substring(1, query_old.length()-1);
        //System.out.println(query);
        Map<String, String> result = new HashMap<>();
        for (String param : query.split(",")) {
            String[] entry = param.split(":");
            //System.out.println(entry[0] + entry[1]);
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    } 
    public static void main(String[] args) throws IOException {
        try {
            // parse connection config from "resources/application.yaml"
            try {
                // connect to database
                connector = new DatabaseConnector(connectConfig);
                library = new LibraryManagementSystemImpl(connector);
                System.out.println("Successfully init class Main.");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            // connect to database
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            /* do somethings */
            // 创建HTTP服务器，监听指定端口
            // 这里是8000，建议不要80端口，容易和其他的撞
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

            // 添加handler，这里就绑定到/card路由
            // 所以localhost:8000/card是会有handler来处理
            server.createContext("/card", new CardHandler());
            server.createContext("/borrow", new BorrowHanler());
            server.createContext("/book", new BookHandler());

            // 启动服务器
            server.start();

            // 标识一下，这样才知道我的后端启动了（确信
            System.out.println("Server is listening on port 8000");
            // release database connection handler
            // if (connector.release()) {
            //     log.info("Success to release connection.");
            // } else {
            //     log.warning("Failed to release connection.");
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static class CardHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                System.out.println("1");
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
            
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            // String response = "[{\"id\": 1, \"name\": \"John Doe\", \"department\": \"Computer Science\", \"type\": \"Student\"}," +
            //         "{\"id\": 2, \"name\": \"Jane Smith\", \"department\": \"Electrical Engineering\", \"type\": \"Faculty\"}]";
            ApiResult result = library.showCards();
            String response = "[";
            CardList resCardList = (CardList) result.payload;
            for (int i=0; i<resCardList.getCount(); i++) {
                response += "{";
                response += "\"id\": " + resCardList.getCards().get(i).getCardId() +", ";
                response += "\"name\": \"" + resCardList.getCards().get(i).getName() + "\", ";
                response += "\"department\": \"" + resCardList.getCards().get(i).getDepartment() + "\", ";
                response += "\"type\": \"" + resCardList.getCards().get(i).getType().toStr() + "\"";
                response += "}";
                if (i != resCardList.getCount()-1) {
                    response += ", ";
                }
            }
            response += "]";
            // 写
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            Map<String, String> cardtypeMap = new HashMap<String, String>();
            cardtypeMap.put("Student", "S");
            cardtypeMap.put("Teacher", "T");
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            // 看看读到了啥
            System.out .println("Received POST request to create card with data: " + requestBodyBuilder.toString());
            
            // 实际处理可能会更复杂点
            String response = requestBodyBuilder.toString();
            Map<String, String> Cardinfo = PostToMap(response);
            System.out.println(Cardinfo);
            Card card = new Card();
            String cardid, cardname, department, cardtype;
            cardid = Cardinfo.get("\"id\"");
            cardname = Cardinfo.get("\"name\"");
            department = Cardinfo.get("\"department\"");
            cardtype = Cardinfo.get("\"type\"");
            //System.out.println(Cardinfo.get("name") + Cardinfo.get("department") + cardtypeMap.get(Cardinfo.get("type")))
            
            if (cardid == null) {
                card.setName(cardname.substring(1, cardname.length()-1));
                card.setDepartment(department.substring(1, department.length()-1));
                card.setType(Card.CardType.values(cardtypeMap.get(cardtype.substring(1, cardtype.length()-1))));
                library.registerCard(card);
            } else {
                if (cardname == null)
                    library.removeCard(Integer.valueOf(cardid));
                else {
                    card.setCardId(Integer.valueOf(cardid));
                    card.setName(cardname.substring(1, cardname.length()-1));
                    card.setDepartment(department.substring(1, department.length()-1));
                    card.setType(Card.CardType.values(cardtypeMap.get(cardtype.substring(1, cardtype.length()-1))));
                    library.modifyCardInfo(card);
                }
            }
            //妈的这里的type搞出来是"学生"，你得把它转成"S"
            
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);    
        }
}
    static class BorrowHanler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            String requestMethod = exchange.getRequestMethod();
            if (requestMethod.equals("GET")) {
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                handleOptionsRequest(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            //获取请求参数
            Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
            ApiResult result = library.showBorrowHistory(Integer.valueOf(query.get("cardID")));
            BorrowHistories resBorrowHistories = (BorrowHistories) result.payload;
            String response = "[";
            for (int i=0; i<resBorrowHistories.getCount(); i++) {
                BorrowHistories.Item o = resBorrowHistories.getItems().get(i);
                response += "{";
                response += "\"cardID\": " + o.getCardId() + ", ";
                response += "\"bookID\": " + o.getBookId() + ", ";
                response += "\"borrowTime\": \"" + o.getBorrowTime() + "\", ";
                response += "\"returnTime\": \"" + o.getReturnTime() + "\"";
                response += "}";
                if (i != resBorrowHistories.getCount()-1) {
                    response += ", ";
                }
            }
            response += "]";
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            InputStream requestBody = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            System.out.println("Received POST request to create borrow with data: " + requestBodyBuilder.toString());
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("Borrow created successfully".getBytes());
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) {
            throw new UnsupportedOperationException("Unimplemented method 'handleOptionsRequest'");
        }
    }

    static class BookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                System.out.println("1");
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
            
        }
        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            BookQueryConditions conditions = new BookQueryConditions();
            Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
            System.out.println(query.toString());
            conditions.setCategory(query.get("category"));
            conditions.setTitle(query.get("title"));
            conditions.setPress(query.get("press"));
            conditions.setMinPublishYear(query.get("minPublishYear") == null ? -999 : Integer.valueOf(query.get("minPublishYear")));
            conditions.setMaxPublishYear(query.get("maxPublishYear") == null ? 9999 : Integer.valueOf(query.get("maxPublishYear")));
            conditions.setAuthor(query.get("author"));
            conditions.setMinPrice(query.get("minPrice") == null ? -999 : Double.valueOf(query.get("minPrice")));
            conditions.setMaxPrice(query.get("maxPrice") == null ? 9999 : Double.valueOf(query.get("maxPrice")));
            ApiResult queryResult = library.queryBook(conditions);
            BookQueryResults resbookResults = (BookQueryResults) queryResult.payload;

            String response = "[";
            for (int i=0; i<resbookResults.getCount(); i++) {
                Book b = resbookResults.getResults().get(i);
                response += "{";
                response += "\"book_id\": " + b.getBookId() +", ";
                response += "\"category\": \"" +b.getCategory() + "\", ";
                response += "\"title\": \"" + b.getTitle() + "\", ";
                response += "\"press\": \"" + b.getPress() + "\", ";
                response += "\"publish_year\": " +b.getPublishYear()+ ", ";
                response += "\"author\": \"" + b.getAuthor()+ "\", ";
                response += "\"price\": "+ b.getPrice()+ ", ";
                response += "\"stock\": " + b.getStock(); 
                response += "}";
                if (i != resbookResults.getCount()-1) {
                    response += ", ";
                }
            }
            response += "]";
            // 写
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            String path = exchange.getRequestURI().getPath();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            // 看看读到了啥
            System.out .println("Received POST request to create card with data: " + requestBodyBuilder.toString());
            
            // 实际处理可能会更复杂点
            String response = requestBodyBuilder.toString();
            Map<String, String> BookInfo = PostToMap(response);
            String book_id = BookInfo.get("\"book_id\"");
            String category = BookInfo.get("\"category\"");
            String title = BookInfo.get("\"title\"");
            String press = BookInfo.get("\"press\"");
            String publish_year = BookInfo.get("\"publish_year\"");
            String author = BookInfo.get("\"author\"");
            String price = BookInfo.get("\"price\"");
            String stock = BookInfo.get("\"stock\"");
            String card_id = BookInfo.get("\"card_id\"");
            if (path.equals("/book" )) {
                Book b = new Book();
                b.setCategory(category.substring(1,category.length()-1));
                b.setTitle(title.substring(1, title.length()-1));
                b.setPress(press.substring(1, press.length()-1));
                b.setPublishYear(Integer.parseInt(publish_year.substring(1, publish_year.length()-1)));
                b.setAuthor(author.substring(1, author.length()-1));
                b.setPrice(Double.parseDouble(price.substring(1, price.length()-1)));
                b.setStock(Integer.parseInt(stock.substring(1, stock.length()-1)));
                library.storeBook(b);
             } else if (path.equals("/book/modify")) {
                Book b = new Book();
                b.setBookId(Integer.parseInt(book_id));
                b.setCategory(category.substring(1,category.length()-1));
                b.setTitle(title.substring(1, title.length()-1));
                b.setPress(press.substring(1, press.length()-1));
                b.setPublishYear(Integer.parseInt(publish_year));
                b.setAuthor(author.substring(1, author.length()-1));
                b.setPrice(Double.parseDouble(price));
                b.setStock(Integer.parseInt(stock));
                library.modifyBookInfo(b);
             } else if (path.equals("/book/stock")) {
                System.out.println(book_id + stock);
                library.incBookStock(Integer.parseInt(book_id), Integer.parseInt(stock));
             } else if (path.equals("/book/remove")) {
                library.removeBook(Integer.parseInt(book_id));
             } else if (path.equals("/book/borrow")) {
                Borrow borrow = new Borrow();
                borrow.setCardId(Integer.parseInt(card_id));
                borrow.setBookId(Integer.parseInt(book_id));
                borrow.resetBorrowTime();
                borrow.setReturnTime(0);
                System.out.println(borrow.toString());
                library.borrowBook(borrow);
             } else if (path.equals("/book/return")) {
                Borrow borrow = new Borrow();
                borrow.setCardId(Integer.parseInt(card_id));
                borrow.setBookId(Integer.parseInt(book_id));
                borrow.resetReturnTime();
                library.returnBook(borrow);
             }
            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码200
            exchange.sendResponseHeaders(200, 0);
        
            // 剩下三个和GET一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // 响应状态码200
            exchange.sendResponseHeaders(204, 0);    
        }
    }

}
    