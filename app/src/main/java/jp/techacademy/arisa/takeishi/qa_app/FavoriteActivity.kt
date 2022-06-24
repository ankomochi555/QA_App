package jp.techacademy.arisa.takeishi.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
//import kotlinx.android.synthetic.main.app_bar_main.*
//import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.activity_favorite.*

class FavoriteActivity : AppCompatActivity() {

    private var mGenre = 0
    //プロパティとしてFirebaseへのアクセスに必要な
    // DatabaseReferenceクラスと、ListView、QuestionクラスのArrayList、QuestionsListAdapterを定義
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mFavoriteDatabaseReference: DatabaseReference
    private lateinit var  mQuestionArrayList: ArrayList<Question> //〇追加         お気に入りに入っているもののQuestionUidだけをいれる　Adapterにセットする
    private lateinit var mFavoriteAdapter: QuestionsListAdapter //〇追加
    //private lateinit var mAdapter: QuestionDetailListAdapter //〇追加　6/16　
    //private lateinit var mFavoriteQuestionUidList: ArrayList<String> //〇追加 6/16　Questionであってる?別のものを作成する必要がある??

    //Realmtimde DatabseのChildEventListenerと同じ役割
    //違いとしては、Listenerを削除するときに、Listener自身のインスタンスに対してremoveをするところ
    // snapshotListener?.remove() で一つ前のリスナーを消す
    private var snapshotListener: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        mFavoriteDatabaseReference = mDatabaseReference.child(ContentsPATH)

        // ListViewの準備
        mFavoriteAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>() //〇追加
       // mFavoriteAdapter.notifyDataSetChanged()

        //質問一覧画面でリストをタップしたらその質問の詳細画面に飛ぶようにしたいので、ListViewのsetOnItemClickListenerメソッドでリスナーを登録し、
        // リスナーの中で質問に相当するQuestionのインスタンスを渡してQuestionDetailActivityに遷移させる
        listView.setOnItemClickListener{parent, view, position, id ->
            // Favorite(Question)のインスタンスをQuestionDetailActivityに渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position]) //★QuestionDetailActivityでget keyはfavoriteにする??
            startActivity(intent)
        }

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す onResumeでよんでいるため
//        mQuestionArrayList.clear()
//        mFavoriteAdapter.setQuestionArrayList(mQuestionArrayList) //〇QuestionListAdapterから変更
//        listView.adapter = mFavoriteAdapter
//
//        // 一つ前のリスナーを消す
//        snapshotListener?.remove()
//
//        snapshotListener = FirebaseFirestore.getInstance()
//            .collection(FavoritePATH) //collection("contents")で取得するべきcollectionを設定
//            .whereEqualTo("genre", mGenre) //whereEqualTo("genre", mGenre)がクエリ   genreが現在選択されているものだけ取得するようにしている
//            .addSnapshotListener { querySnapshot, firebaseFirestoreException -> //addSnapshotListenerは設定したcollectionとクエリに該当するデータで変更、追加、削除があった場合、全件取得し直すもの
//                if (firebaseFirestoreException != null) { //第二引数のfirebaseFirestoreExceptionについて。これは、nullableで、nullだった場合は成功, null出なかったら場合は失敗
//                    // 取得エラー
//                    return@addSnapshotListener
//                }
//                var questions = listOf<Question>()
//                val results = querySnapshot?.toObjects(FirestoreQuestion::class.java) //取得したデータをList<FireStoreQuestion>に変換できる。直接クラスに変換できるので、のちの処理が楽になる。
//                results?.also {
//                    questions = it.map { firestoreQuestion ->
//                        val bytes =
//                            if (firestoreQuestion.image.isNotEmpty()) {
//                                Base64.decode(firestoreQuestion.image, Base64.DEFAULT)
//                            } else {
//                                byteArrayOf()
//                            }
//                        Question(firestoreQuestion.title, firestoreQuestion.body, firestoreQuestion.name, firestoreQuestion.uid,
//                            firestoreQuestion.id, firestoreQuestion.genre, bytes, firestoreQuestion.answers)
//                    }
//                }
//                //取得したデータを用いて、ListViewの表示の更新
//                mQuestionArrayList.clear()
//                mQuestionArrayList.addAll(questions)
//                mFavoriteAdapter.notifyDataSetChanged()
//            }
    }

    override fun onResume() { //初期化
        super.onResume()
        mFavoriteDatabaseReference.addChildEventListener(mEventListener)
        //mDatabaseReference.addChildEventListener(mEventListener)
        mQuestionArrayList.clear()
        mFavoriteAdapter.notifyDataSetChanged()
        mFavoriteAdapter.setQuestionArrayList(mQuestionArrayList) //〇QuestionListAdapterから
        listView.adapter = mFavoriteAdapter
    }

    //データがここで取れていない
    private val mEventListener =
        object : ChildEventListener { //変化があった時に呼ばれるリスナー　mEventListenerは質問投稿時の動き
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) { //登録　
                // ジャンルごとにまず取れるので、それを１つずつ見て中身を抽出する
                //val map = dataSnapshot.value as? HashMap<*, *> //★Map<*, *>どういう型でもいいよ dataSnapshotはHashMapと同じKEYと値

                val map = dataSnapshot.value as HashMap<String, HashMap<String, String>>?
                if (map != null) { //forループ内で何をしている??
                    for ((key, value) in map){
                        val map2 = HashMap<String, String>()
                        for ((key2, value2) in value as HashMap<*, *>){
                            map2[key2 as String] = value2 as String
                        }
                        val question = convertMapToQuestion(key as String, map2)

                        if (mFavoriteArrayList.contains(question.questionUid)){ //お気に入りリスト
                            mQuestionArrayList.add(question)
                        }
                    }
                }
                mFavoriteAdapter.notifyDataSetChanged()
            }

            // HashMapのデータをQuestionオブジェクトに変換する
            private fun convertMapToQuestion(questionUid: String, map: HashMap<String, String>):
            Question {
                //keyをもとにそれぞれの値を出す　dataSnapshot　KEYと値で情報持っている Questionの中のもの全部
                val title = map["title"] as? String ?: ""
                val body = map["body"] as? String ?: ""
                val name = map["name"] as? String ?: ""
                val uid = map["uid"] as? String ?: ""
                val imageString = map["image"] ?: ""
                val bytes =
                    if (imageString.isNotEmpty()) {
                        Base64.decode(imageString, Base64.DEFAULT)
                    } else {
                        byteArrayOf()
                    }

                //val answerUid = dataSnapshot.key ?: "" // dataSnapshotは全部持ってきて、mFavoriteArrayListに入っている同じ条件の情報を引っ張ってくる

                val answerArrayList = ArrayList<Answer>()

                val answerMap = map["answers"] as HashMap<*, *>?
                if (answerMap != null) {
                    for (key in answerMap.keys) {
                        val temp = answerMap[key] as Map<*, *>
                        val answerBody = temp["body"] as? String ?: ""
                        val answerName = temp["name"] as? String ?: ""
                        val answerUid = temp["uid"] as? String ?: ""
                        val answer = Answer(answerBody, answerName, answerUid, key as String)
                    }
                }

                return Question(title, body, name, uid, questionUid, mGenre, bytes, answerArrayList)
                //取ってきた情報をmQuestionにいれる　mQuestionを定義する必要がある
                //val mQuestion: Question mQuestionの定義はここ??
                // どうやってmQuestiobに情報をいれる??val answerMap = map["answers"] as HashMap<*, *>?を使う??
//                for (answer in answerMap!!.keys) {
//                }
//
//                // map["body"] []にkeyを入れると値が取れる
//                val body = map["body"] as? String ?: ""
//                val name = map["name"] as? String ?: ""
//                val uid = map["uid"] as? String ?: ""
//
//                val answer = Answer(body, name, uid, answerUid)
//                answerArrayList.add(answer) //リストを定義する
//                mFavoriteAdapter.notifyDataSetChanged()
            }

            //★下の4つの関数の役割　使わなくても記載する必要がある
            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) { //上書き
                val map = dataSnapshot.value as Map<*, *>

                //変更があったQuestionを探す
                for (question in mQuestionArrayList) {
                    if (dataSnapshot.key == question.questionUid) {
                        //このアプリで変更がある可能性があるのは回答(Answer)のみ
                        question.answers.clear()
                        val answerMap = map["answers"] as? Map<*, *>
                        if (answerMap != null) {
                            for (key in answerMap.keys) {
                                val temp = answerMap[key] as HashMap<*, *>
                                val answerBody = temp["body"] as? String ?: ""
                                val answerName = temp["name"] as? String ?: ""
                                val answerUid = temp["uid"] as? String ?: ""
                                val answer = Answer(answerBody, answerName, answerUid,key as String)

                                question.answers.add(answer)
                            }
                        }
                        mFavoriteAdapter.notifyDataSetChanged()
                    }
                }

            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }

}