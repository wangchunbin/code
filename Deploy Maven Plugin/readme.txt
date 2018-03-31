本maven插件内容如下:
 一.数据库部署插件:
 1.第一步，首先确认Deploy Maven Plugin插件已经安装到本地Maven仓库中。然后设置用到该插件的项目的pom.xml文件中插件数据库连接参数及数据文件存放目录。(可以是绝对路径也可以是相对路径，想对路径相对于pom.xml所在路径)。	
 2.第二步，将整理好的sql脚本及excel数据文件， 放入在pom.xml设置的dataDir目录下,然后重命名文件名,设置文件被处理的顺序：按1、2、3...编号设置文件名第一个字符,文件名首字符数字越小，则插件优先处理该文件。
        另外注意文件内容规则：
   1)sql文件中的SQL语句使用#END结尾,因为插件是使用#来切割出文件中每条SQL，注意Java JDBC执行的insert、create table语句结尾没有分号,但存储过程、触发器有;
   2)execl文件中每个sheet第一行第一个单元格填写要插入数据的表名， 第二行填写表列名，要插入表多少列就填写多少个单元格。 第三行开始填写要插入的数据，注意要设置好单元格格式，第一行和第二行为文本格式，第三行开始每列根据数据类型设置单元格格式，否则插入的数据有问题;	       
 3.第三步，选中项目右键执行"mvn com.deploy:deploy-maven-plugin:1.0:DBInit"命令，运行插件.

二、程序全量/增量部署插件:
       类似Jenkins，结合git、gitlab或者github、maven、tomcat、jdk环境，实现web程序全量及增量部署。
       相关配置请见pom_run.xml,执行"mvn com.deploy:deploy-maven-plugin:1.0:Deploy"命令运行。