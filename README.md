# RCONLib

<!-- TOC -->

* [RCONLib](#rconlib)
* [简介](#简介)
* [添加依赖](#添加依赖)
* [使用示例](#使用示例)

<!-- TOC -->

# 简介

* Kotlin实现的RCON库可以连接服务器并远程管理.
 
* 一个实例可以连接一个服务器, 连接多个服务器需要创建多个实例.
 
* 代码太简单直接翻源码就可以知道怎么使用了

* 最低JDK为11

# 添加依赖

```gradle
maven { url 'https://jitpack.io' }
```

```gradle
dependencies {
    implementation 'com.github.DangoTown:RCONLib:$version'
}
```

# 使用示例

```kotlin
fun main() {
    val rcon = RCon()
    rcon.authenticate("this is a password")

    val response = rcon.executeCommand("list")

    println(response)  // or println(response.body) # override toString method to directly print body filed
}
```