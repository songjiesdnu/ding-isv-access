# ding-isv-access


rm -rf ~/antx.properties
mvn clean package -Dmaven.test.skip=true

http://30.26.118.6:8080/ding-isv-access/checkpreload.htm

数据库名称是大写的

# ding-isv-access


步骤一:创建DB
1.不要调整sql文件中创建表的顺序.quartz是有表外检约束的。和quartz相关的table名称都是大写的

创建DB和表的过程中出现,Unknown character set: 'utf8mb4'
1.更新mysql版本至>5.5
2.使用db_sql.utf8.sql





