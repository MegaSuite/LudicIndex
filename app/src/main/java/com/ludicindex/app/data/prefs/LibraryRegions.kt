package com.ludicindex.app.data.prefs

/** A selectable region for a catalog request. */
data class RegionOption(val code: String, val label: String)

/**
 * Regions the subscription catalogs actually serve. Every entry below was verified live to return
 * data, so the picker can never offer a combination that yields an empty library.
 */
object LibraryRegions {

    const val DEFAULT_GAME_PASS_MARKET = "US"
    const val DEFAULT_PS_PLUS_LOCALE = "en-us"

    /** Verified against `catalog.gamepass.com` (all return 560–629 console games). */
    val GAME_PASS_MARKETS: List<RegionOption> = listOf(
        RegionOption("HK", "香港"),
        RegionOption("TW", "台湾"),
        RegionOption("JP", "日本"),
        RegionOption("KR", "韩国"),
        RegionOption("SG", "新加坡"),
        RegionOption("IN", "印度"),
        RegionOption("US", "美国"),
        RegionOption("CA", "加拿大"),
        RegionOption("MX", "墨西哥"),
        RegionOption("BR", "巴西"),
        RegionOption("AR", "阿根廷"),
        RegionOption("CL", "智利"),
        RegionOption("CO", "哥伦比亚"),
        RegionOption("GB", "英国"),
        RegionOption("IE", "爱尔兰"),
        RegionOption("FR", "法国"),
        RegionOption("DE", "德国"),
        RegionOption("AT", "奥地利"),
        RegionOption("CH", "瑞士"),
        RegionOption("IT", "意大利"),
        RegionOption("ES", "西班牙"),
        RegionOption("PT", "葡萄牙"),
        RegionOption("NL", "荷兰"),
        RegionOption("BE", "比利时"),
        RegionOption("PL", "波兰"),
        RegionOption("CZ", "捷克"),
        RegionOption("HU", "匈牙利"),
        RegionOption("GR", "希腊"),
        RegionOption("SE", "瑞典"),
        RegionOption("NO", "挪威"),
        RegionOption("DK", "丹麦"),
        RegionOption("FI", "芬兰"),
        RegionOption("TR", "土耳其"),
        RegionOption("SA", "沙特阿拉伯"),
        RegionOption("AE", "阿联酋"),
        RegionOption("IL", "以色列"),
        RegionOption("AU", "澳大利亚"),
        RegionOption("NZ", "新西兰"),
        RegionOption("ZA", "南非"),
    )

    /**
     * Verified against the PlayStation game finder (`bin/imagic/gameslist`). Locales the store does
     * not serve for PS Plus (zh-hans-cn, pt-br, ru-ru, …) are deliberately absent.
     */
    val PS_PLUS_LOCALES: List<RegionOption> = listOf(
        RegionOption("zh-hant-hk", "香港（繁体中文）"),
        RegionOption("zh-hans-hk", "香港（简体中文）"),
        RegionOption("en-hk", "香港（英文）"),
        RegionOption("zh-hant-tw", "台湾（繁体中文）"),
        RegionOption("ja-jp", "日本"),
        RegionOption("ko-kr", "韩国"),
        RegionOption("en-sg", "新加坡"),
        RegionOption("en-my", "马来西亚"),
        RegionOption("en-in", "印度"),
        RegionOption("en-ph", "菲律宾"),
        RegionOption("en-th", "泰国"),
        RegionOption("en-id", "印度尼西亚"),
        RegionOption("en-vn", "越南"),
        RegionOption("en-us", "美国"),
        RegionOption("en-ca", "加拿大（英文）"),
        RegionOption("fr-ca", "加拿大（法语）"),
        RegionOption("es-mx", "墨西哥"),
        RegionOption("es-ar", "阿根廷"),
        RegionOption("es-cl", "智利"),
        RegionOption("es-co", "哥伦比亚"),
        RegionOption("es-pe", "秘鲁"),
        RegionOption("es-cr", "哥斯达黎加"),
        RegionOption("es-ec", "厄瓜多尔"),
        RegionOption("es-gt", "危地马拉"),
        RegionOption("es-pa", "巴拿马"),
        RegionOption("es-uy", "乌拉圭"),
        RegionOption("es-bo", "玻利维亚"),
        RegionOption("es-sv", "萨尔瓦多"),
        RegionOption("es-hn", "洪都拉斯"),
        RegionOption("es-ni", "尼加拉瓜"),
        RegionOption("es-py", "巴拉圭"),
        RegionOption("pt-pt", "葡萄牙"),
        RegionOption("en-gb", "英国"),
        RegionOption("en-ie", "爱尔兰"),
        RegionOption("fr-fr", "法国"),
        RegionOption("fr-be", "比利时（法语）"),
        RegionOption("fr-ch", "瑞士（法语）"),
        RegionOption("de-de", "德国"),
        RegionOption("de-at", "奥地利"),
        RegionOption("de-ch", "瑞士（德语）"),
        RegionOption("it-it", "意大利"),
        RegionOption("it-ch", "瑞士（意大利语）"),
        RegionOption("es-es", "西班牙"),
        RegionOption("nl-nl", "荷兰"),
        RegionOption("nl-be", "比利时（荷兰语）"),
        RegionOption("pl-pl", "波兰"),
        RegionOption("sv-se", "瑞典"),
        RegionOption("da-dk", "丹麦"),
        RegionOption("fi-fi", "芬兰"),
        RegionOption("tr-tr", "土耳其"),
        RegionOption("ar-ae", "阿联酋（阿语）"),
        RegionOption("ar-sa", "沙特阿拉伯（阿语）"),
        RegionOption("en-ae", "阿联酋（英文）"),
        RegionOption("en-sa", "沙特阿拉伯（英文）"),
        RegionOption("en-bh", "巴林"),
        RegionOption("en-kw", "科威特"),
        RegionOption("en-qa", "卡塔尔"),
        RegionOption("en-om", "阿曼"),
        RegionOption("en-lb", "黎巴嫩"),
        RegionOption("en-au", "澳大利亚"),
        RegionOption("en-nz", "新西兰"),
        RegionOption("en-za", "南非"),
    )

    fun gamePassLabel(code: String): String =
        GAME_PASS_MARKETS.firstOrNull { it.code == code }?.label ?: code

    fun psPlusLabel(code: String): String =
        PS_PLUS_LOCALES.firstOrNull { it.code == code }?.label ?: code
}
