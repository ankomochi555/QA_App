package jp.techacademy.arisa.takeishi.qa_app

// Firebaseにユーザの表示名を保存するパス
const val UsersPATH = "contents"

// Firebaseに質問を保存するバス
const val ContentsPATH = "contents"

// Firebaseに回答を保存するパス
const val AnswersPATH = "answers"

// Firebaseにお気に入りを保存するパス
const val FavoritePATH = "favorite"


// Preferenceに表示名を保存する時のキー
const val NameKEY = "name"

//お気に入り一覧の保存先
var  mFavoriteArrayList = ArrayList<String>()