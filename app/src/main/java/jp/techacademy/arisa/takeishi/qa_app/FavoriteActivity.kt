package jp.techacademy.arisa.takeishi.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class FavoriteActivity : AppCompatActivity() {

    private var mGenre = 0
    //プロパティとしてFirebaseへのアクセスに必要な
    // DatabaseReferenceクラスと、ListView、QuestionクラスのArrayList、QuestionsListAdapterを定義
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var  mFavoriteArrayList: ArrayList<Question> //〇追加
    private lateinit var mFavoriteAdapter: FavoriteListAdapter //〇追加

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
        mFavoriteAdapter = FavoriteListAdapter(this)
        mFavoriteArrayList = ArrayList<Question>() //〇追加
        mFavoriteAdapter.notifyDataSetChanged()

        //質問一覧画面でリストをタップしたらその質問の詳細画面に飛ぶようにしたいので、ListViewのsetOnItemClickListenerメソッドでリスナーを登録し、
        // リスナーの中で質問に相当するQuestionのインスタンスを渡してQuestionDetailActivityに遷移させる
        listView.setOnItemClickListener{parent, view, position, id ->
            // Favorite(Question)のインスタンスをQuestionDetailActivityに渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mFavoriteArrayList[position]) //★QuestionDetailActivityでget keyはfavoriteにする??
            startActivity(intent)
        }

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mFavoriteArrayList.clear()
        mFavoriteAdapter.setFavoriteArrayList(mFavoriteArrayList) //〇QuestionListAdapterから変更
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
                mFavoriteArrayList.clear()
                mFavoriteArrayList.addAll(questions)
                mFavoriteAdapter.notifyDataSetChanged()
            }
    }

//    override fun onNavigationItemSelected(item: MenuItem): Boolean {
//
//        return true
//    }
}