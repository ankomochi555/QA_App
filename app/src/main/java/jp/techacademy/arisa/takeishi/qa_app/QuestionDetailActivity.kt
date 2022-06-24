package jp.techacademy.arisa.takeishi.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
//import jp.techacademy.arisa.takeishi.qa_app.databinding.ActivityQuestionDetailBinding
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    //private lateinit var binding: ActivityQuestionDetailBinding

    private lateinit var mQuestion: Question

    //private lateinit var mFavorite: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private var mIsFavorite: Boolean = false

    //更新　onChild全部入れる　お気に入り登録のときだけ追加のみaddedのなかに追加処理を書く　お気に入り追加は別で定義
    private val mFavoriteListener = object : ChildEventListener { //お気に入り登録時の動き
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) { //登録　/ 更新
                //binding.favoriteImageBtn.setImageResource(R.drawable.ic_star)
                favoriteImageBtn.setImageResource(R.drawable.ic_star)
                mIsFavorite = true

//

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    private val mEventListener =
        object : ChildEventListener { //変化があった時に呼ばれるリスナー　mEventListenerは質問投稿時の動き
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) { //登録　
                val map = dataSnapshot.value as Map<*, *> //★Map<*, *>どういう型でもいいよ

                val answerUid = dataSnapshot.key ?: ""

                for (answer in mQuestion.answers) {
                    // 同じAnswerUidのものが存在しているときは何もしない
                    if (answerUid == answer.answerUid) {
                        return
                    }
                }

                val body = map["body"] as? String ?: ""
                val name = map["name"] as? String ?: ""
                val uid = map["uid"] as? String ?: ""

                val answer = Answer(body, name, uid, answerUid)
                mQuestion.answers.add(answer)
                mAdapter.notifyDataSetChanged()
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

    //onCreateメソッドでは渡ってきたQuestionクラスのインスタンスを保持し、タイトルを設定。
    // そして、ListViewの準備をする。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)
//        binding = ActivityQuestionDetailBinding.inflate(layoutInflater)
//        val view = binding.root
//        setContentView(view)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question


        title = mQuestion.title


        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()


        //FABをタップしたらログインしていなければログイン画面に遷移させ、
        // ログインしていれば後ほど作成する回答作成画面に遷移させる準備をしておく
        fab.setOnClickListener { //fab 質問詳細画面のプラスボタン
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // QuestionDetailActivityからAnswerSendActivityに遷移する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        //〇　ログインしている場合、質問詳細画面に「お気に入り」ボタンを表示する　
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) { //ログインしていない場合、表示しない
            favoriteImageBtn.visibility = View.INVISIBLE
        } else { //ログインしている場合、表示
            //favoriteImageBtn.setVisibility(View.VISIBLE)

            mIsFavorite = false
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val mFavoriteRef: DatabaseReference =
                dataBaseReference.child(FavoritePATH).child(user!!.uid).child(mQuestion.questionUid)
            mFavoriteRef.addChildEventListener(mFavoriteListener) //　お気に入り押したときにmFavoriteListenerリスナーが動く
        }
        //var flg = true
        //val favoriteImageBtn : ImageButton = findViewById(R.id.favoriteImageBtn)
        //binding.favoriteImageBtn.setOnClickListener {
        favoriteImageBtn.setOnClickListener {

            //お気に入りタップで登録/更新

            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val mFavoriteRef: DatabaseReference =
                dataBaseReference.child(FavoritePATH).child(user!!.uid).child(mQuestion.questionUid)

            if (mIsFavorite) {
                //binding.favoriteImageBtn.setImageResource(R.drawable.ic_star)
                favoriteImageBtn.setImageResource(R.drawable.ic_star)
                mIsFavorite = false
                mFavoriteArrayList.remove(mQuestion.questionUid)


            } else {
                //binding.favoriteImageBtn.setImageResource(R.drawable.ic_star_border)
                favoriteImageBtn.setImageResource(R.drawable.ic_star_border)
                mIsFavorite = true
                //mFavoriteRef!!.removeEventListener(mFavoriteListener)

                //データを保存する何か
                val data = HashMap<String, String>() //HashMap key 値
                data["genre"] = mQuestion.genre.toString() //保存するもの
                mFavoriteRef.setValue(data) //mFavoriteRef保存する場所
                mFavoriteArrayList.add(mQuestion.questionUid)
//                mFavoriteRef.removeValue() //登録解除
//                mFavoriteArrayList.remove(mQuestion.questionUid)
            }

            /*
                if (mFavoriteRef == null) {
                    mFavoriteRef.addChildEventListener(mFavoriteListener) //　お気に入り押したときにmFavoriteListenerリスナーが動く
                    favoriteImageBtn.setImageResource(R.drawable.ic_star) //ImageViewの変更
                } else { //お気に入り登録済みであれば、削除
                    mFavoriteRef!!.removeEventListener(mFavoriteListener)
                    favoriteImageBtn.setImageResource(R.drawable.ic_star_border) //ImageViewの変更
                }
                 */


            /* 選択したジャンルにリスナーを登録する MainActivity
                if (mGenreRef != null) {
                    mGenreRef!!.removeEventListener(mEventListener) 
                }
                //参照して登録
                mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
                mGenreRef!!.addChildEventListener(mEventListener)

                 */
        }

        /*//Chapter7-7.6
            // お気に入り状態を取得
            val isFavorite = FavoriteShop.findBy(data.id) != null

            // 白抜きの星マークの画像を指定
            favoriteImageView.apply {
                setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border) // Picassoというライブラリを使ってImageVIewに画像をはめ込む
                setOnClickListener {
                    if (isFavorite) {
                        onClickDeleteFavorite?.invoke(data)
                    } else {
                        onClickAddFavorite?.invoke(data)
                    }
                    notifyItemChanged(position)
                }
            }
             */


        //Firebaseへのリスナーの登録が重要
        // 回答作成画面から戻ってきた時にその回答を表示させるために登録しておく ★ .child(mQuestion.genre.toString()). 変数.プロパティ.toString()型に変更

    }
}