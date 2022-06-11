package jp.techacademy.arisa.takeishi.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
// findViewById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mGenre = 0

    //プロパティとしてFirebaseへのアクセスに必要な
    // DatabaseReferenceクラスと、ListView、QuestionクラスのArrayList、QuestionsListAdapterを定義
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    //Realmtimde DatabseのChildEventListenerと同じ役割
    //違いとしては、Listenerを削除するときに、Listener自身のインスタンスに対してremoveをするところ
    // snapshotListener?.remove() で一つ前のリスナーを消す
    private var snapshotListener: ListenerRegistration? = null

    /*
    private var mGenreRef: DatabaseReference? = null

    //QuestionsListAdapterにデータを設定するために、Firebaseからデータを取得する必要がある
    // データに追加・変化があった時に受け取るChildEventListenerを作成する
    private val mEventListener = object : ChildEventListener {

        //onChildAdded 要素が追加されたとき、つまり質問が追加された時に呼ばれるメソッド
        // この中でQuestionクラスとAnswerを作成し、ArrayListに追加する
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String> //★これは何をしている
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) { //★if,else内　ここでは何が起きている
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answer"] as Map<String,String>? //★これは何をしている
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String> //★これは何をしている
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                    mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        //onChildChanged  要素に変化があった時に呼ばれる
        //質問に対して回答が投稿された時に呼ばれる
        //このメソッドが呼ばれたら、変化があった質問に対応するQuestionクラスのインスタンスで
        // 保持している回答のArrayListをいったんクリアし、取得した回答を設定する
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String,String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) { //　★forとは繰り返しの処理　for (要素 in コレクション) 処理
                            // 　ここで起きていることは？
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody =  temp["body"] ?: ""
                            val answerName =  temp["name"] ?: ""
                            val answerUid =  temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //質問や回答を投稿する際にログインしていなければログイン画面を表示するようにしたいので、まずはログインしているかどうかを確認
        // idがtoolbarがインポート宣言により取得されているので
        // id名でActionBarのサポートを依頼
        setSupportActionBar(toolbar)

        // fabにClickリスナーを登録
        //FABのListenerの箇所でQuestionSendActivityを起動するようにする
        fab.setOnClickListener { view ->

            // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
            if (mGenre == 0) {
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            } else {

            }
            // ログイン済みのユーザーを取得する
            //FirebaseクラスのgetAuthメソッドで認証情報であるAuthDataクラスのインスタンスを取得できる
            //この戻り値がnullである場合はログインしていないことになるので、その場合はログイン画面へ遷移するように実装
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしていなければログイン画面に遷移させる
            if (user == null){
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }


        // ナビゲーションドロワーの設定
        // ★R.string.app_nameを2回書くのはなぜ
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)


        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        //質問一覧画面でリストをタップしたらその質問の詳細画面に飛ぶようにしたいので、ListViewのsetOnItemClickListenerメソッドでリスナーを登録し、
        // リスナーの中で質問に相当するQuestionのインスタンスを渡してQuestionDetailActivityに遷移させる
        listView.setOnItemClickListener{parent, view, position, id ->
            // QuestionのインスタンスをQuestionDetailActivityに渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }


    //onResume  mGenre == 0 の場合（すなわち初回起動時）には、最初の選択肢である「1:趣味」を表示するようにしている
    //FABのListenerでも、念のため mGenre == 0 のチェックを行っている
    override fun onResume() {
        super.onResume()

        // 1:趣味を既定の選択とする
        if (mGenre == 0) {
            onNavigationItemSelected(nav_view.menu.getItem(0))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //右上のメニューから設定画面へ進むようにする
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_favorite) { //ログインしている場合にボタンを表示
            toolbar.title = getString(R.string.menu_favorite_label)
            mGenre = 1
        } else if (id == R.id.nav_hobby) {
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 2
        } else if (id == R.id.nav_life) {
            toolbar.title = getString(R.string.menu_life_label)
            mGenre = 3
        } else if (id == R.id.nav_health) {
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 4
        } else if (id == R.id.nav_compter){
            toolbar.title = getString(R.string.menu_compter_label)
            mGenre = 5
        }
        drawer_layout.closeDrawer(GravityCompat.START) //START :サイズを変更せずに、オブジェクトをコンテナの先頭でx軸の位置にプッシュする

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        // 一つ前のリスナーを消す
        snapshotListener?.remove()


        // 選択したジャンルにリスナーを登録する

        /*
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener) //★登録処理をしている?
        }
        //参照して登録
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        mGenreRef!!.addChildEventListener(mEventListener)

         */

        snapshotListener = FirebaseFirestore.getInstance()
            .collection(ContentsPATH) //collection("contents")で取得するべきcollectionを設定
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
                mAdapter.notifyDataSetChanged()
            }

        return true
    }

}