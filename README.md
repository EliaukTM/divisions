# 中国行政区划
省，市，区三级行政区划代码的 java 库

## 简介
* 可用[爬虫](https://github.com/CrawlScript/WebCollector)爬取[民政部](http://www.mca.gov.cn/article/sj/xzqh/)最新的行政区划代码。(不调用爬虫，默认使用 2019 年 5月份的数据)
* 利用 [GB2260.java](https://github.com/cn/GB2260.java) 来实现区划代码的使用

## Usage

### 引入依赖
```shell
mvn clean package
```

```xml
<dependencies>
    ...
    <dependency>
        <groupId>com.haymai</groupId>
        <artifactId>divisions</artifactId>
        <version>0.1.1-SNAPSHOT</version>
    </dependency>
    ...
</dependencies>
```

### 爬取最新的行政区划
```java
McaCrawler.run()
```

### GB2260 的使用

```java
GB2260 gb = new GB2260(); // with default revision 2019
```

Interface for GB2260.

#### .getDivision(code)

Get division for the given code.

```java
Division division = gb.getDivision("110105")
// 北京市 北京市 朝阳区

division.getName()
// 朝阳区
division.getCode()
// 110105

division.getProvince()
// 北京市
division.getPrefecture()
// 北京市

division.toString()
// 北京市 北京市 朝阳区
```

#### .getProvinces()

Return a list of provinces in Division data structure.

```java
gb.getProvinces()
```

#### .getPrefectures(code)

Return a list of prefecture level cities in Division data structure.

```java
gb.getPrefectures("110000")
```

#### .getCounties(code)

Return a list of counties in Division data structure.

```java
gb.getCounties("110100")
```