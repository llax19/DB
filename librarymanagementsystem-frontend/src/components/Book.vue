<!-- TODO: YOUR CODE HERE -->
<template>
    <!-- <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理</div>    
        <p style="margin-left: 40px; margin-top: 50px;">这里是图书管理页面，请尝试实现。</p>

    </el-scrollbar> -->
    <!-- 标题和搜索框 -->
    <el-scrollbar height="100%" style="width: 100%;">
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理
            <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right;" clearable />
        </div>
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">
            <el-button type="primary"  @click="this.SeachBookVisible = true" >筛选图书</el-button>
        </div>
        <!-- 书籍卡片显示区 -->
        <div style="display: flex;flex-wrap: wrap; justify-content: start;">

            <!-- 书籍卡片 -->
            <div style="position: relative;" class="bookBox" v-for="book in books" v-show="book.title.includes(toSearch) || book.author.includes(toSearch)" :key="book.book_id">
                <div>
                    <!-- 卡片标题 -->
                    <div style="font-size: 25px; font-weight: bold;">No. {{ book.book_id }}</div>

                    <el-divider />

                    <!-- 卡片内容 -->
                    <div style="margin-left: 10px; text-align: start; font-size: 16px;">
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">类别：</span>{{ book.category }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">书名：</span>{{ book.title }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">出版社：</span>{{ book.press }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">出版年份：</span>{{ book.publish_year }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">作者：</span>{{ book.author }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">价格：</span>{{ book.price }}</p>
                        <p style="padding: 2.5px;"><span style="font-weight: bold;">库存：</span>{{ book.stock }}</p>
                    </div>

                    <el-divider />

                    <!-- 卡片操作 -->
                    <div style="position:absolute;margin-bottom: 30px;bottom:0;margin-left:48px;">
                        <el-button type="primary" :icon="Edit" @click="this.toModifyInfo.book_id = book.book_id, this.toModifyInfo.category = book.category,
                this.toModifyInfo.title = book.title, this.toModifyInfo.press = book.press,this.toModifyInfo.publish_year = book.publish_year, this.toModifyInfo.author = book.author,
                this.toModifyInfo.price = book.price, this.toModifyInfo.stock = book.stock,
                this.modifyBookVisible = true" circle />
                        <el-button type="danger" :icon="Delete" 
                            @click="this.toRemove = book.book_id, this.removeBookVisible = true"
                            style="margin-left: 45px;" circle />
                        <el-button type="success" :icon="Promotion"
                            @click="this.toBorrow.book_id = book.book_id, this.borrowBookVisible = true"
                            style="margin-left: 45px;" circle />
                    </div>

                </div>
            </div>

            <!-- 新建借书证卡片 -->
            <el-button class="newbookBox"
            @click="newBookInfo.category = '', newBookInfo.title = '', newBookInfo.press = '', newBookInfo.publish_year = ''
            , newBookInfo.author='', newBookInfo.price='', newBookInfo.stock='', newBookVisible = true">
            <el-icon style="height: 50px; width: 50px;">
                <Plus style="height: 100%; width: 100%;" />
            </el-icon>
            </el-button>
        </div>


        <!-- 新建书籍对话框 -->
        <el-dialog v-model="newBookVisible" title="新建书籍" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：
                <el-input v-model="newBookInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="newBookInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="newBookInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input v-model="newBookInfo.publish_year" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="newBookInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input v-model="newBookInfo.price" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                库存：
                <el-input v-model="newBookInfo.stock" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBook"
                        :disabled="newBookInfo.category.length === 0 || newBookInfo.title.length === 0 ||
                                    newBookInfo.press.length === 0 || newBookInfo.publish_year === 0 ||
                                    newBookInfo.author.length === 0 || newBookInfo.price.length === 0 ||
                                    newBookInfo.stock.length === 0">确定</el-button>
                </span>
            </template>
            
        </el-dialog>

        <!-- 修改信息对话框 -->   
        <el-dialog v-model="modifyBookVisible" :title="'修改信息(书籍ID: ' + this.toModifyInfo.book_id + ')'" width="30%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：
                <el-input v-model="toModifyInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="toModifyInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="toModifyInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <!-- <el-input v-model="toModifyInfo.publish_year" style="width: 12.5vw;" clearable />
                //如需要输入整数去掉precision就可 -->
                <el-input v-model.number="toModifyInfo.publish_year" style="width: 12.5vw;" clearable
                maxlength="9"></el-input>
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="toModifyInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input v-model.number="toModifyInfo.price" type="number" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                库存：
                <el-input v-model.number="toModifyInfo.stock" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyBook"
                        :disabled="toModifyInfo.category.length === 0 || toModifyInfo.title.length === 0 ||
                                    toModifyInfo.press.length === 0 || toModifyInfo.publish_year === 0 ||
                                    toModifyInfo.author.length === 0 || toModifyInfo.price.length === 0">修改信息</el-button>
                    <el-button type="primary" @click="ConfirmIncreaseStock"
                    :disabled="toModifyInfo.stock.length === 0">增减库存</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 删除书籍对话框 -->  
        <el-dialog v-model="removeBookVisible" title="删除书籍" width="30%">
            <span>确定删除<span style="font-weight: bold;">{{ toRemove }}号书籍</span>吗？</span>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveBook">
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 借/还书对话框-->
        <el-dialog v-model="borrowBookVisible" title="借/还书" width="30%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                借书证ID:
                <el-input v-model.number="toBorrow.card_id" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="borrowBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmBorrowBook"
                        :disabled="toBorrow.card_id === 0">确定借书</el-button>
                    <el-button type="primary" @click="ConfirmReturnBook"
                        :disabled="toBorrow.card_id === 0">确定还书</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 筛选书籍对话框 -->
        <el-dialog v-model="SeachBookVisible" title="筛选书籍" width="30%"
            align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：
                <el-input v-model="condition.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="condition.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="condition.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                最小出版年份：
                <el-input v-model.number="condition.minPublishYear" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                最大出版年份：
                <el-input v-model.number="condition.maxPublishYear" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="condition.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                最低价格：
                <el-input v-model.number="condition.minPrice" type="number" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                最高价格：
                <el-input v-model.number="condition.maxPrice" type="number" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                排序依据：
                <el-select v-model="toModifyInfo.sortBy" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in sortBy" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                排序方式：
                <el-select v-model="toModifyInfo.sortOrder" size="middle" style="width: 12.5vw;">
                    <el-option v-for="type in sortOrder" :key="type.value" :label="type.label" :value="type.value" />
                </el-select>
            </div>

            <template #footer>
                <span>
                    <el-button @click="SeachBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="QueryBooks">确定</el-button>
                </span>
            </template>
        </el-dialog>
    </el-scrollbar>
</template>


<script>
import { Delete, Edit, Search, Promotion} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
    data() {
        return {
            books: [{
                book_id:1,
                category: "CS",
                title: "C++ Primer",
                press: "Tsinghua University Press",
                publish_year: 2021,
                author: "Stanley B. Lippman",
                price: 79.0,
                stock: 10
            },
            {
                book_id:2,
                category: "CS2",
                title: "C++ Primer",
                press: "Tsinghua University Press",
                publish_year: 2021,
                author: "Stanley B. Lippman",
                price: 79.0,
                stock: 10
            }
            ],
            Delete,
            Edit,
            Search,
            Promotion,
            toSearch: '',
            newBookVisible: false,
            removeBookVisible: false,
            modifyBookVisible: false,
            borrowBookVisible: false,
            SeachBookVisible:false,
            newBookInfo: {
                category: '',
                title: '',
                press: '',
                publish_year: 0,
                author: '',
                price: 0.0,
                stock:0
            },
            toRemove: 0,
            toModifyInfo: {
                book_id: 0,
                category: '',
                title: '',
                press: '',
                publish_year: 0,
                author: '',
                price: 0.0,
                stock:0
            },
            toBorrow: {
                card_id: 0,
                book_id: 0,
                borrowTime: "2024.03.04 21:48",
                returnTime: "2024.03.04 21:49"
            },
            toSearchBook: {
                category: '',
                title: '',
                press: '',
                publish_year: null,
                author: '',
                price: null,
                stock:null
            },
            condition:{ 
                category: null, 
                title: null, 
                press:null, 
                minPublishYear: null, 
                maxPublishYear: null, 
                author:null, 
                minPrice: null, 
                maxPrice: null, 
                sortBy: 'book_id', 
                sortOrder:'asc'
            },
            sortBy: [
                { label: '书籍ID', value: 'book_id' },
                { label: '类别', value: 'category' },
                { label: '书名', value: 'title' },
                { label: '出版社', value: 'press' },
                { label: '出版年份', value: 'publish_year' },
                { label: '作者', value: 'author' },
                { label: '价格', value: 'price' },
                { label: '库存', value: 'stock' }
            ],
            sortOrder: [
                { label: '升序', value: 'asc' },
                { label: '降序', value: 'desc' }
            ]

        }
    },
    methods: {
        ConfirmNewBook() {
            axios.post('/book',
                {//请求体
                    category: this.newBookInfo.category,
                    title: this.newBookInfo.title,
                    press: this.newBookInfo.press,
                    publish_year: this.newBookInfo.publish_year,
                    author: this.newBookInfo.author,
                    price: this.newBookInfo.price,
                    stock: this.newBookInfo.stock
                })
                .then(response =>{
                    ElMessage.success("图书新建成功") // 显示消息提醒
                    this.newBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        ConfirmModifyBook() {
            axios.post('/book/modify',
                {//请求体
                    book_id: this.toModifyInfo.book_id,
                    category: this.toModifyInfo.category,
                    title: this.toModifyInfo.title,
                    press: this.toModifyInfo.press,
                    publish_year: this.toModifyInfo.publish_year,
                    author: this.toModifyInfo.author,
                    price: this.toModifyInfo.price,
                    stock: this.toModifyInfo.stock
                })
                .then(response =>{
                    ElMessage.success("图书信息修改成功") // 显示消息提醒
                    this.modifyBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },

        ConfirmRemoveBook() {
            axios.post('/book/remove',
                {//请求体
                    book_id: this.toRemove
                })
                .then(response =>{
                    ElMessage.success("图书删除成功") //
                    this.removeBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        ConfirmBorrowBook() {
            axios.post('/book/borrow',
                {//请求体
                    card_id: this.toBorrow.card_id,
                    book_id: this.toBorrow.book_id
                })
                .then(response =>{
                    ElMessage.success("借书成功") // 显示消息提醒
                    this.borrowBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        ConfirmReturnBook() {
            axios.post('/book/return',
                {//请求体
                    card_id: this.toBorrow.card_id,
                    book_id: this.toBorrow.book_id
                })
                .then(response =>{
                    ElMessage.success("还书成功") // 显示消息提醒
                    this.borrowBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        ConfirmIncreaseStock() {
            axios.post('/book/stock',
                {//请求体
                    book_id: this.toModifyInfo.book_id,
                    stock: this.toModifyInfo.stock
                })
                .then(response =>{
                    ElMessage.success("库存修改成功") // 显示消息提醒
                    this.modifyBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        async QueryBooks() {
            this.books = []
            let response =  await axios.get('/book', { params:{
                category: this.condition.category,
                title: this.condition.title,
                press: this.condition.press,
                minPublishYear: this.condition.minPublishYear,
                maxPublishYear: this.condition.maxPublishYear,
                author: this.condition.author,
                minPrice: this.condition.minPrice,
                maxPrice: this.condition.maxPrice,
                sortBy: this.condition.sortBy,
                sortOrder: this.condition.sortOrder
            }})
            let books = response.data
            books.forEach(book => {
                this.books.push(book)
            });
            this.SeachBookVisible = false
        }
    },
    mounted() {
        this.QueryBooks()
    }
}
</script>
<style scoped>
.bookBox {
    height: 450px;
    width: 300px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    text-align: center;
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
}

.newbookBox {
    height: 450px;
    width: 300px;
    margin-top: 40px;
    margin-left: 27.5px;
    margin-right: 10px;
    padding: 7.5px;
    padding-right: 10px;
    padding-top: 15px;
    box-shadow: 0 4px 8px 0 rgba(0, 0, 0, 0.2), 0 6px 20px 0 rgba(0, 0, 0, 0.19);
    text-align: center;
}
</style>