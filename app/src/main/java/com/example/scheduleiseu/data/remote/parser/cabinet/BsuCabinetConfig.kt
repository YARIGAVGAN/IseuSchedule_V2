package com.example.scheduleiseu.data.remote.parser.cabinet

object BsuCabinetConfig {
    const val loginUrl = "https://student.bsu.by/login?ReturnUrl=%2fPersonalCabinet%2fNews"
    const val studProgressUrl = "https://student.bsu.by/PersonalCabinet/StudProgress"
    const val cabinetUrl = "https://student.bsu.by/PersonalCabinet/News"
    const val photoUrl = "https://student.bsu.by/Photo/Photo.aspx"
    const val userAgent = "Mozilla/5.0"

    const val loginTitleMarker = "Вход в личный кабинет студента"
    const val cabinetTitleMarker = "Личный кабинет студента БГУ"
    const val cabinetLandingPath = "/personalcabinet/news"
    const val progressPath = "/PersonalCabinet/StudProgress"
    const val photoPath = "/PersonalCabinet/stbd"

    const val loginResultSelector = "#ctl00_ContentPlaceHolder0_lbLoginResult"
    const val primaryDisplayNameSelector = "#ctl00_ctl00_LoginView1_LoginFIO"
    const val secondaryDisplayNameSelector = "#ctl00_ctl00_ContentPlaceHolder0_lbFIO1"
    const val cabinetMenuSelector = ".Sub1 a[href], .Sub2 a[href]"
    const val progressSemestersSelector =
        "#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_tblSemester"
    const val progressTableSelector =
        "#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_tblProgress"
    const val facultySelector =
        "#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_lbStudFacultet"
    const val groupInfoSelector =
        "#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_lbStudKurs"
    const val averageScoreSelector =
        "#ctl00_ctl00_ContentPlaceHolder0_ContentPlaceHolder1_ctlStudProgress1_lbStudBall"

    val jsPostBackRegex: Regex = Regex("""__doPostBack\('([^']*)','([^']*)'\)""")
    val gradeRegex: Regex = Regex("""\b([0-9]|10)\b""")
}
