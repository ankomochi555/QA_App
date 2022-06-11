package jp.techacademy.arisa.takeishi.qa_app

import java.io.Serializable

//Answerクラスは、後述のQuestionクラスの中で保持されているクラスとなり、Questionクラスは、Serializableを実装させるようにする。
// このようなSerializableを実装したとあるクラスの中で保持されるクラスもSerializableを実装させる必要がある
//質問の回答のモデルクラス
//変数名	内容
//body	Firebaseから取得した回答本文
//name	Firebaseから取得した回答者の名前
//uid	Firebaseから取得した回答者のUID
//answerUid	Firebaseから取得した回答のUID

class Answer (val body: String, val name: String, val uid: String, val answerUid: String) : Serializable