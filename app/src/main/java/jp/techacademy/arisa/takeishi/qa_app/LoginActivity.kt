package jp.techacademy.arisa.takeishi.qa_app


import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {
    //Firebase関連でFirebaseAuthクラスと、
    // 処理の完了を受け取るリスナーであるOnCompleteListenerクラスをアカウント作成処理とログイン処理用の2つ
    // そしてデータベースへの読み書きに必要なDatabaseReferenceクラスを定義
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListner: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private lateinit var mDataBaseReference: DatabaseReference

    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false


    //onCreateメソッドでは、以下を実装
    // データベースへのリファレンスを取得
    //FirebaseAuthクラスのインスタンスを取得
    //アカウント作成処理のリスナーを作成
    //ログイン処理のリスナーを作成
    //タイトルバーのタイトルを変更
    //アカウント作成ボタンとログインボタンのOnClickListenerを設定
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // FirebaseAuthのオブジェクトを取得する
        mAuth = FirebaseAuth.getInstance()

        // アカウント作成処理のリスナー　Firebaseのアカウント作成処理はOnCompleteListenerクラスで受け取る
        //アカウント作成が成功した際にはそのままログイン処理を行うため、loginメソッドを呼び出す。
        // アカウント作成に失敗した場合は、Snackbarでエラーの旨を表示
        mCreateAccountListner = OnCompleteListener { task ->
            if (task.isSuccessful){
                // 成功した場合
                // ログインを行う
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password)
            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.create_account_failure_message), Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // ログイン処理のリスナー Firebaseのログイン処理もOnCompleteListenerクラスで受け取る
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                //ログインに成功したときはmIsCreateAccountを使ってアカウント作成ボタンを押してからのログイン処理か、
                // ログインボタンをタップの場合かで処理を分けます
                // ログイン処理が成功した場合
                //アカウント作成ボタンを押した場合は表示名をFirebaseとPreferenceに保存 ★どこのことを説明している??
                //Firebaseは、データをKeyとValueの組み合わせで保存します。DatabaseReferenceが指し示すKeyにValueを保存するには setValue メソッドを使用 ★上に同じ
                val user = mAuth.currentUser
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid) //★childとは？どこを指している

                if (mIsCreateAccount) { //★ここでのifとelseの関係は？
                    // アカウント作成の時は表示名をFirebaseに保存する
                    val name = nameText.text.toString()

                    val data = HashMap<String, String>() //★HashMapとは？
                    data["name"] = name //[]で囲む理由とは？
                    userRef.setValue(data)

                    // 表示名をPreferenceに保存する
                    saveName(name)
                } else {
                    //ログインボタンをタップしたときは、Firebaseから表示名を取得してPreferenceに保存 ★ここの説明であってる??
                    //Firebaseからデータを一度だけ取得する場合はDatabaseReferenceクラスが実装しているQueryクラスのaddListenerForSingleValueEventメソッドを使用
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>? //★この行はどういう意味 asの役割
                            saveName(data!!["name"] as String)
                        }

                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                //ログインに失敗した場合は、Snackbarでエラーの旨を表示し、処理中に表示していたダイアログを非表示にする。
                // 最後に finish() メソッドで LoginActivity を閉じる。

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE

                // Activityを閉じる
                finish()

            } else {
                // ログイン処理が失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        //アカウント作成ボタンをタップした時の処理
        // タイトルの設定
        title = getString(R.string.login_title)

        createButton.setOnClickListener { v->
            //アカウント作成ボタンをタップした時に、
            // InputMethodManager の hideSoftInputFromWindow メソッドを呼び出してキーボードを閉じ、
            // ログイン時に表示名を保存するようにmIsCreateAccountにtrueを設定
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager //★InputMethodManagerとは？　キーボードを閉じるなにか
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {
                // ログイン時に表示名を保存するようにフラグを立てる
                mIsCreateAccount = true

                //createAccountメソッドを呼び出し、アカウント作成処理を開始
                createAccount(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()
            }
        }

        //ログインボタンのタップした時に
        // 同様にキーボードを閉じ、loginメソッドを呼び出してログイン処理を開始
        loginButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // フラグを落としておく
                mIsCreateAccount = false

                login(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG).show()

            }
        }
    }

    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // FirebaseAuthクラスのcreateUserWithEmailAndPasswordメソッドでアカウントを作成する
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListner)
    }

    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // FirebaseAuthクラスのsignInWithEmailAndPasswordメソッドでログイン処理をする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    private fun saveName(name: String) {
        // 引数で受け取った表示名をPreferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        //忘れずにcommitメソッドを呼び出して保存処理を反映
        editor.commit()
    }
}