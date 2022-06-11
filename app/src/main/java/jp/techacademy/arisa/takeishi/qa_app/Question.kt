package jp.techacademy.arisa.takeishi.qa_app

import java.io.Serializable
import java.util.ArrayList

//Firebaseから取得した質問のデータを保持するモデルクラス
//プロパティはvalで定義し、コンストラクタで値を設定
// Serializableクラスを実装している理由はIntentでデータを渡せるようにするため

class Question (val title: String, val body: String, val name: String, val uid: String, val questionUid: String, val genre: Int, bytes: ByteArray, val answers: ArrayList<Answer>) : Serializable {
    val imageBytes: ByteArray //バイト配列とは、データ型がバイト(byte)型で値が-128～127までの配列

    init { //init 初期化で使われる関数で、オブジェクトを作った時に定義
        imageBytes = bytes.clone()
    }
}