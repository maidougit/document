#<center>redis之key设计</center>
---
#阐述#
&emsp;&emsp;规范都是无形之产生的，没有规范，就没有了准心，也容易引发各种问题。缓存数据库redis也是一样的。key的应该有一定的规范才容易实施，不容易产生问题。
#key设计#
<p>&emsp;&emsp;key的一个格式约定：object-type:id:field。用":"分隔域，用"."作为单词间的连接，如"comment:12345:reply.to"。不推荐含义不清的key和特别长的key。</p>
<p>&emsp;&emsp;一般的设计方法如下： 1: 把表名转换为key前缀 如, tag: 2: 第2段放置用于区分区key的字段--对应mysql中的主键的列名,如userid 3: 第3段放置主键值,如2,3,4...., a , b ,c 4: 第4段,写要存储的列名。</p>
> for example
例如用户表 user, 转换为key-value存储：

| userid        | username        | password |email|
| :------------- |:-------------:| :-----:|:---:|
| 9     | maicheng |  11111 |maicheng@163.com|

```
set user:userid:9:username list<br/>
set user:userid:9:password 111111
set user:userid:9:email   lisi@163.com
```
##
> for example</br>
keys user:userid:9*</br>
如果另一个列也常常被用来查找，比如username，则也要相应的生成一条按照该列为主的key-value，例如：

>user:username:lisi:uid 9

