package com.haymai.division.crawler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.haymai.division.DivisionFileUtils;
import com.haymai.division.crawler.utils.AmapUtils;

import cn.edu.hfut.dmic.webcollector.model.CrawlDatum;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.plugin.net.OkHttpRequester;
import cn.edu.hfut.dmic.webcollector.plugin.rocks.BreadthCrawler;
import okhttp3.Request;

/**
 * Description: 中国行政区划爬虫，来源于民政部 <br/>
 * <code>origin url : http://www.mca.gov.cn/article/sj/xzqh<code/>
 *
 * @author haymai
 * @version 1.0
 * @date 6/13/2019 4:00 PM
 * @since JDK 1.8
 */
public class McaCrawler extends BreadthCrawler {

    private static final String START_URL = "http://www.mca.gov.cn/article/sj/xzqh/";

    private static final Pattern REAL_PAGE_URL = Pattern.compile(".*?window.location.href=\"(.*?)\"(.*?)",
            Pattern.DOTALL);

    private static final String SHTML_FORMAT = "%s(\\d+)/(\\d+)/(\\d+).shtml";

    private static final String HTML_FORMAT = "%s(\\d+)/(\\d+)-(\\d+)/(\\d+).html";

    private static final Pattern URL_CODE = Pattern.compile(String.format(HTML_FORMAT, START_URL));

    public static final String SINGLE_LINE_FORMAT = "%s\t%s";

    private String amapKey;

    public static class MyRequester extends OkHttpRequester {

        String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36";
        String cookie = "yd_cookie=7616e7e1-61cb-4cc3ad590d3ce14207ab271cfe65adbc7641; fpv=a8632a0a60a1efb9868780105b371326; "
                + "yd_captcha_token=ycnp405Dpyo5S6iwnVU2cKafUvhUzbaLPFEYkXZtSTO23gtWbhaJzQCBl3HvY35bTdm5Kgfeonzifkf+upg7PQ%3D%3D; "
                + "_gscu_190942352=603947273gros534; _gscbrs_190942352=1; _gscs_190942352=60926988mxsbwd34|pv:3";

        @Override
        public Request.Builder createRequestBuilder(CrawlDatum crawlDatum) {
            // 这里使用的是OkHttp中的Request.Builder
            // 可以参考OkHttp的文档来修改请求头
            return super.createRequestBuilder(crawlDatum).header("User-Agent", userAgent).header("Cookie", cookie);
        }

    }

    /**
     * @param crawlPath
     *            crawlPath is the path of the directory which maintains information of this crawler
     * @param autoParse
     *            if autoParse is true,BreadthCrawler will auto extract links which match regex rules from pag
     */
    public McaCrawler(String crawlPath, boolean autoParse, String amapKey) {
        super(crawlPath, autoParse);
        // 设置请求插件
        setRequester(new MyRequester());
        /* start pages */
        this.addSeed(START_URL);
        // 打开页面的过滤
        this.addRegex(START_URL + "\\d+/.*?");
        /* do not fetch jpg|png|gif */
        this.addRegex("-.*\\/(wap/).*");
        setThreads(2);
        getConf().setTopN(20);
        this.amapKey = amapKey;
    }

    @Override
    public void visit(Page page, CrawlDatums next) {
        /* if page is news page */
        if (page.matchUrl(String.format(SHTML_FORMAT, START_URL))) {
            Matcher matcher = REAL_PAGE_URL.matcher(page.html());
            if (matcher.matches()) {
                next.add(matcher.group(1));
            }
            return;
        }
        Matcher matcher = URL_CODE.matcher(page.url());
        if (!matcher.matches()) {
            return;
        }
        String fileCode = matcher.group(4);
        // 文件已存在不爬了
        if (DivisionFileUtils.isFileExists(fileCode)) {
            return;
        }
        List<String> result = new ArrayList<>();
        Elements trs = page.select("tr");
        Set<String> prefectureCodes = new HashSet<>();
        Map<String, String> provinces = new HashMap<>(34);
        String lastCode = "";
        List<String> needExtractDivisions = new ArrayList<>();
        for (Element tr : trs) {
            Elements tds = tr.select("td");
            if (tds.size() < 3) {
                continue;
            }
            String code = tds.get(1).text();
            String name = tds.get(2).text();
            if (!StringUtil.isBlank(code) && !StringUtil.isBlank(name) && StringUtil.isNumeric(code)
                    && code.length() == 6) {
                if (code.endsWith("00")) {
                    if ((lastCode.endsWith("0000") && code.endsWith("0000"))) {
                        needExtractDivisions.add(lastCode);
                    }
                    if (lastCode.endsWith("00") && !lastCode.endsWith("0000")) {
                        needExtractDivisions.add(lastCode);
                    }
                    prefectureCodes.add(code);
                }
                lastCode = code;
                String parentPrefectureCode = code.substring(0, 4) + "00";
                if (!code.endsWith("0") && !prefectureCodes.contains(parentPrefectureCode)) {
                    String provinceCode = code.substring(0, 2) + "0000";
                    String provinceName = provinces.get(provinceCode);
                    if (provinceName.endsWith("自治区")) {
                        result.add(String.format(SINGLE_LINE_FORMAT, parentPrefectureCode, "自治区直辖县级行政区划"));
                    } else if (provinceName.endsWith("市")) {
                        result.add(String.format(SINGLE_LINE_FORMAT, parentPrefectureCode, "县"));
                    } else {
                        result.add(String.format(SINGLE_LINE_FORMAT, parentPrefectureCode, "省直辖县级行政区划"));
                    }
                    prefectureCodes.add(parentPrefectureCode);
                }
                result.add(String.format(SINGLE_LINE_FORMAT, code, name));
                if (code.endsWith("0000")) {
                    provinces.put(code, name);
                }
                // 过滤直辖市
                if (code.endsWith("0000") && name.endsWith("市")) {
                    String code1;
                    code1 = code.substring(0, 2) + "0100";
                    result.add(String.format(SINGLE_LINE_FORMAT, code1, name));
                    prefectureCodes.add(code1);
                }
            }
        }
        if (!result.isEmpty()) {
            String ultimateDivision = result.get(result.size() - 1);
            String ultimateDivisionCode = ultimateDivision.split("\t")[0];
            if (ultimateDivisionCode.endsWith("00")) {
                needExtractDivisions.add(ultimateDivisionCode);
            }
            if (!StringUtil.isBlank(amapKey) && !needExtractDivisions.isEmpty()) {
                result.addAll(AmapUtils.handleExtractDivision(amapKey, needExtractDivisions));
            }
            DivisionFileUtils.saveFile(fileCode, result);
        }
    }

    public static void run() throws Exception {
        McaCrawler crawler = new McaCrawler("crawl", true, null);
        crawler.getConf().setExecuteInterval(5000);
        crawler.start(4);
    }

    public static void run(String amapKey) throws Exception {
        McaCrawler crawler = new McaCrawler("crawl", true, amapKey);
        crawler.getConf().setExecuteInterval(5000);
        crawler.getConf().setMaxExecuteCount(1);
        crawler.start(4);
    }

}