package jp.techacademy.arisa.takeishi.qa_app

import java.util.*

class FirestoreQuestion {
    var id = UUID.randomUUID().toString()
    var title = ""
    var body = ""
    var name = ""
    var uid = ""
    var image = ""
    var genre = 0
    var answers: ArrayList<Answer> = arrayListOf()
}