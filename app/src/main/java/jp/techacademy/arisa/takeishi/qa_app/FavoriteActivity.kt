package jp.techacademy.arisa.takeishi.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class FavoriteActivity : AppCompatActivity() {

    private var mGenre = 0
    //プロパティとしてFirebaseへのアクセスに必要な
    // DatabaseReferenceクラスと、ListView、QuestionクラスのArrayList、QuestionsListAdapterを定義
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mFavoriteDatabaseReference: DatabaseReference
    private lateinit var  mQuestionArrayList: ArrayList<Question> //〇追加         お気に入りに入っているもののQuestionUidだけをいれる　Adapterにセットする
    private lateinit var mFavoriteAdapter: QuestionsListAdapter //〇追加

    //Realmtimde DatabseのChildEventListenerと同じ役割
    //違いとしては、Listenerを削除するときに、Listener自身のインスタンスに対してremoveをするところ
    // snapshotListener?.remove() で一つ前のリスナーを消す
    private var snapshotListener: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mFavoriteAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>() //〇追加
        mFavoriteAdapter.notifyDataSetChanged()

        //質問一覧画面でリストをタップしたらその質問の詳細画面に飛ぶようにしたいので、ListViewのsetOnItemClickListenerメソッドでリスナーを登録し、
        // リスナーの中で質問に相当するQuestionのインスタンスを渡してQuestionDetailActivityに遷移させる
        listView.setOnItemClickListener{parent, view, position, id ->
            // Favorite(Question)のインスタンスをQuestionDetailActivityに渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position]) //★QuestionDetailActivityでget keyはfavoriteにする??
            startActivity(intent)
        }

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mFavoriteAdapter.setQuestionArrayList(mQuestionArrayList) //〇QuestionListAdapterから変更
        listView.adapter = mFavoriteAdapter

        // 一つ前のリスナーを消す
        snapshotListener?.remove()

        snapshotListener = FirebaseFirestore.getInstance()
            .collection(FavoritePATH) //collection("contents")で取得するべきcollectionを設定
            .whereEqualTo("genre", mGenre) //whereEqualTo("genre", mGenre)がクエリ   genreが現在選択されているものだけ取得するようにしている
            .addSnapshotListener { querySnapshot, firebaseFirestoreException -> //addSnapshotListenerは設定したcollectionとクエリに該当するデータで変更、追加、削除があった場合、全件取得し直すもの
                if (firebaseFirestoreException != null) { //第二引数のfirebaseFirestoreExceptionについて。これは、nullableで、nullだった場合は成功, null出なかったら場合は失敗
                    // 取得エラー
                    return@addSnapshotListener
                }
                var questions = listOf<Question>()
                val results = querySnapshot?.toObjects(FirestoreQuestion::class.java) //取得したデータをList<FireStoreQuestion>に変換できる。直接クラスに変換できるので、のちの処理が楽になる。
                results?.also {
                    questions = it.map { firestoreQuestion ->
                        val bytes =
                            if (firestoreQuestion.image.isNotEmpty()) {
                                Base64.decode(firestoreQuestion.image, Base64.DEFAULT)
                            } else {
                                byteArrayOf()
                            }
                        Question(firestoreQuestion.title, firestoreQuestion.body, firestoreQuestion.name, firestoreQuestion.uid,
                            firestoreQuestion.id, firestoreQuestion.genre, bytes, firestoreQuestion.answers)
                    }
                }
                //取得したデータを用いて、ListViewの表示の更新
                mQuestionArrayList.clear()
                mQuestionArrayList.addAll(questions)
                mFavoriteAdapter.notifyDataSetChanged()
            }
    }

    override fun onResume() {
        super.onResume()
        mFavoriteDatabaseReference.addChildEventListener(mEventListener)
        mQuestionArrayList.clear()
        mFavoriteAdapter.notifyDataSetChanged()
        mFavoriteAdapter.setQuestionArrayList(mQuestionArrayList) //〇QuestionListAdapterから
        listView.adapter = mFavoriteAdapter
    }

    private val mEventListener =
        object : ChildEventListener { //変化があった時に呼ばれるリスナー　mEventListenerは質問投稿時の動き
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) { //登録　
                val map = dataSnapshot.value as Map<*, *> //★Map<*, *>どういう型でもいいよ dataSnapshotはHashMapと同じKEYと値

                //keyをもとにそれぞれの値を出す　dataSnapshot　KEYと値で情報持っている Questionの中のもの全部
                val mTitle = map["title"] as? String ?: ""
                val mBody = map["body"] as? String ?: ""
                val mName = map["name"] as? String ?: ""
                val mUid = map["uid"] as? String ?: ""
                val mQuestionUid = map["questionUid"] as? String ?: ""
                val mGenre = map["genre"] as? Int ?: ""
                val mBytes = map["bytes"] as? ByteArray ?: ""
                //val mAnswers: map["answers"] as? ArrayList<Answer> ?: ""


                //val title: String, val body: String, val name: String, val uid: String, val questionUid: String, val genre: Int, bytes: ByteArray, val answers: ArrayList<Answer>)

                val answerUid = dataSnapshot.key ?: "" // dataSnapshotは全部持ってきて、mFavoriteArrayListに入っている同じ条件の情報を引っ張ってくる

                val answerArrayList = ArrayList<Answer>()

                val answerMap = map["answers"] as HashMap<*, *>?
                //取ってきた情報をmQuestionにいれる　mQuestionを定義する必要がある
                //val mQuestion: Question mQuestionの定義はここ??
                // どうやってmQuestiobに情報をいれる??val answerMap = map["answers"] as HashMap<*, *>?を使う??
                for (answer in answerMap!!.keys) {
                }

                // map["body"] []にkeyを入れると値が取れる
                val body = map["body"] as? String ?: ""
                val name = map["name"] as? String ?: ""
                val uid = map["uid"] as? String ?: ""

                val answer = Answer(body, name, uid, answerUid)
                answerArrayList.add(answer) //リストを定義する
                mFavoriteAdapter.notifyDataSetChanged()
            }

            //★下の4つの関数の役割　使わなくても記載する必要がある
            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) { //上書き

            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {

            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }

}