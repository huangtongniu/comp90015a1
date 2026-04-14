## instruction



### 项目结构

你可以采用按**功能模块**划分的包（Package）结构：

- **`client` 包**：
  - 放置 `DictionaryClientGUI.java`  以及处理与服务器通信的逻辑类。
- **`server` 包**：
  - 放置服务器的主程序、多线程处理逻辑（如 Thread-per-connection）以及服务器端的 GUI 监控代码 。
- **`protocol` 或 `common` 包**：
  - 由于客户端和服务器需要遵循相同的“消息交换协议” ，你可以把消息格式（如 JSON 解析类或自定义对象）放在这里，供双方共用。
- **`storage` 包**（可选）：
  - 专门负责读写字典文件（数据持久化）的逻辑 。



###  1. 核心技术要求

- **编程语言**：必须使用 **Java** 。禁止使用 RMI 或 JMS 。

- **底层抽象**：必须**显式使用 Socket 和 Threads**，且它们必须是网络通信和并发处理的最低级别抽象 。

- **并发控制**：

  - 服务器必须支持多个客户端同时连接 。
  - 必须实现“**并发读，单写**”机制（例如使用 `ReadWriteLock`） 。
  - 不允许并发读写同一条目 。

  

### 2. 功能要求

服务器需要支持以下五种操作，且所有更改必须实时对所有客户端可见：

- **查询 (Query)**：搜索单词含义，若未找到需报错 。

- **添加 (Add)**：添加新词及含义，若单词已存在或含义为空则报错 。

- **删除 (Remove)**：删除单词及其所有含义 。

- **添加含义 (Add Meaning)**：给已有单词增加新含义，需查重 。

- **更新含义 (Update Meaning)**：替换已有单词的特定含义 。

  

  

### 3. 系统架构与交互

- **架构**：客户端-服务器架构，服务器可采用“每请求一线程”、“每连接一线程”或“线程池”模式 

- **可靠性**：支持 TCP 或 UDP，但若用 UDP 必须自行实现可靠传输机制 。

- **持久化**：字典必须存储在磁盘上（如 JSON 或文件）。如果服务器崩溃或重启，数据不能丢失 。

  

- **GUI**：

  - **客户端**：必须有 GUI 进行各项操作 。
- **服务器端**：必须有 GUI 显示活跃连接数、运行日志，并提供手动启停按钮 。



- 客户端需求

When the client is launched, it creates a TCP socket or a UDP socket bound to the server address
and port number. This socket remains open for the duration of the client-server interaction. All
messages are sent/received through this socket. Do not hardcode port number/IP address.
Additionally, a configurable sleep or delay argument must pe provided to simulate the operation
execution time that will be used to verify the thread-safety. The server must be able to handle
concurrent threads with simulated delay and only allow concurrent READS i.e., at-most 1 client
is able to WRITE to the dictionary while multiple clients are able to READ from the dictionary.
This implies that concurrent READS and WRITES on the same element/entry/value is
prohibited/incompatible and therefore must be handled atomically. Therefore, we may need a
shared lock such as ReadWriteLock to prohibit write operations on the shared data in the
dictionary.



### 4. 关键实现细节

- **启动参数**：
  - **服务器**：`java -jar DictionaryServer.jar <port> <dictionary-file>` 。
  - **客户端**：`java -jar DictionaryClient.jar <server-address> <server-port> <sleep-duration>` 。
- **模拟延迟**：客户端启动时需提供 `sleep-duration` 参数，用于在服务器端模拟操作执行时间，以验证线程安全（并发控制） 。





## 实现指引

### 项目结构优化

建立了标准的 Java 包结构，将文件按照功能模块放置：

client：包含 DictionaryClientGUI.java，负责客户端界面和通信逻辑。
server：包含 DictionaryServer.java，负责服务端 GUI、多线程处理和连接监控。
protocol：包含 Message.java，定义了客户端与服务端交换的消息格式（支持所有操作和延迟模拟）。
storage：包含 DictionaryManager.java，负责字典数据的持久化（dic.txt）和并发控制。



### 核心功能实现

- 并发控制：在 DictionaryManager 中显式使用了 ReentrantReadWriteLock，实现了“并发读、单写”机制，确保多线程下数据的完整性。

- 多线程模型：服务端采用 CachedThreadPool 线程池模式，支持多个客户端同时连接。

- 五大操作：完整实现了查询（Query）、添加单词（Add）、删除单词（Remove）、添加含义（Add Meaning）以及更新含义（Update Meaning）。

- 持久化：所有更改都会实时同步到磁盘文件 dic.txt 中。

- GUI 增强：

  - 服务端：新增了 GUI 界面，可显示实时日志、活跃连接数，并提供手动“启动/停止”按钮。
  - 客户端：保留了原有的 DictionaryClientGUI 结构，并集成了 Socket 通信逻辑。

  

### 启动参数支持

服务端：java server.DictionaryServer <port> <dictionary-file>
客户端：java client.DictionaryClientGUI <server-address> <server-port> <sleep-duration>



### 文件布局

~~~
f:/distribute/Project1/Dictionary/
├── client/
│   └── DictionaryClientGUI.java    (客户端主程序)
├── server/
│   └── DictionaryServer.java        (服务端主程序 + 监控 GUI)
├── protocol/
│   └── Message.java                 (消息协议类)
├── storage/
│   └── DictionaryManager.java       (存储逻辑 + 读写锁控制)
├── dic.txt                          (字典数据文件)
└── README.md                        (项目说明文档)
~~~





### 如何运行

编译： 在根目录下执行：javac -d out client/DictionaryClientGUI.java server/DictionaryServer.java storage/DictionaryManager.java protocol/Message.java
运行服务端： java -cp out server.DictionaryServer 3005 dic.txt
运行客户端： java -cp out client.DictionaryClientGUI localhost 3005 1000 (1000ms 为模拟延迟时间)
我已经清理了旧的碎片文件夹（如 DictionaryClient, DictionaryServer, src 等），并将编译产物统一存放在 out 目录中，现在的结构整洁且完全符合实验要求。
