zooinspector - 中文版
============

本项目是基于**zooinspector**二次开发的zookeeper节点查看器

**原版项目地址**: https://github.com/zzhang5/zooinspector.git

#### 原项目说明:

- 使用异步操作加快读取速度
- 在树查看器中节点按名称排序
- 节点元数据查看器中更可读格式的时间戳和会话id
- 添加下拉菜单以显示最近10个成功连接的zookeeper地址
- 支持节点data viewer中的文本搜索
- 支持节点data viewer的只读模式

#### 优化后功能

- 汉化软件
- 树查看器中增加按节点名称模糊查询节点
- 升级jdk环境1.8
- 升级依赖环境
- 修复弹窗位置居中
- 修复删除节点后在windows环境下显示异常

#### 构建及打包

- $git clone https://github.com/zhangning12138/zooinspector.git
- $cd zooinspector/
- $mvn clean package

#### 运行

linux:

- $chmod +x target/zooinspector-pkg/bin/zooinspector.sh
- $target/zooinspector-pkg/bin/zooinspector.sh

windows:

- 双击 target/zooinspector-pkg/bin/zooinspector.bat
