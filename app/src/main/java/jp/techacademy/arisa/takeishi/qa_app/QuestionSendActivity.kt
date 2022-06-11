package jp.techacademy.arisa.takeishi.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream

//OnClickListenerとCompletionListenerを実装
//メソッド名	内容
//onCreate	渡ってきたジャンルの番号を保持。UIの準備。
//onActivityResult	Intent連携で取得した画像をリサイズしてImageViewに設定。
//onClick	ImageViewとButtonがタップされた時の処理。
//          ImageViewをタップしたときは必要であれば許可を求めるダイアログを表示。
//onRequestPermissionsResult	許可を求めるダイアログからの結果を受け取る。
//showChooser	Intent連携の選択ダイアログを表示する。
//onComplete	Firebaseへの保存完了時に呼ばれる。

class QuestionSendActivity : AppCompatActivity(), View.OnClickListener {

    //パーミッションのダイアログとIntent連携からActivityに戻ってきた時に識別するための定数
    //それぞれこのActivity内で複数のパーミッションの許可のダイアログを出すことがある場合、
    // 複数のActivityから戻ってくることがある場合に識別するための値 何かしらの値が入っていれば問題ない
    companion object {
        private val PERMISSIONS_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    private var mGenre: Int = 0 //ジャンルを保持するプロパティ
    private var mPictureUri:Uri? = null //カメラで撮影した画像を保存するURIを保持するプロパティ


    //onCreate
    // Intentで渡ってきたジャンル番号を取り出してmGenreで保持。
    // タイトルの設定と、リスナーの設定としてUIの準備。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // 渡ってきたジャンルの番号を保持する
        val extras = intent.extras
        mGenre = extras!!.getInt("genre")

        // UIの準備
        title = getString(R.string.question_send_title)

        sendButton.setOnClickListener(this)
        imageView.setOnClickListener(this)
    }

    
    //onActivityResult	Intent連携から戻ってきた時に画像を取得し、ImageViewに設定
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSER_REQUEST_CODE) { //★if文の中では何が起きている??

            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            // 画像を取得
            //dataがnullかdata.getData()の場合はカメラで撮影したときなので画像の取得にmPictureUriを使う
            //data.getData()で取得できた場合はそのURIを使用
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URIからBitmapを取得する
            // ★Bitmapとは　画像を表示したり変更したりする/画像や描画結果をメモリ上に「保存」しておくことができる
            val image: Bitmap
            //★try/catchの関係　例外処理
            try { //try なんらかの処理
                //ContentResolverクラス、InputStreamクラス、BitmapFactoryクラスを使ってURIからBitmapを作成
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri!!)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            } catch (e: Exception) { //catch (変数名: 例外の型 )エラーになった場合の処理
                return
            }

            //取得したBitmapからリサイズして新たなBitmapを作成し、ImageViewに設定
            // 取得したBimapの長辺を500ピクセルにリサイズする
            val imageWidth = image.width
            val imageHeight = image.height
            val scale = Math.min(500.toFloat() / imageWidth, 500.toFloat() / imageHeight) // (1)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val resizeImage = Bitmap.createBitmap(image,0,0, imageWidth, imageHeight, matrix, true)

            // BitmapをImageViewに設定する
            imageView.setImageBitmap(resizeImage)

            mPictureUri = null
        }
    }

    //onClick
    // 添付画像を選択・表示するImageViewと投稿ボタンであるButtonがタップされた時の処理。
    // ImageViewをタップしたときは必要であれば許可を求めるダイアログを表示。
    override fun onClick(v: View) {
        if (v === imageView) {
            // パーミッションの許可状態を確認する
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//Android6.0以降のとき、checkSelfPermissionメソッドで外部ストレージへの書き込みが許可されているか確認
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 許可されている  　Intent連携でギャラリーとカメラを選択するダイアログを表示させるshowChooserメソッドを呼び出す
                    showChooser()
                } else {
                    // 許可されていないので許可ダイアログを表示する
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)

                    return
                }
            } else { //Android5以前の場合はパーミッションの許可状態を確認せずにshowChooserメソッドを呼び出す
                showChooser()
            }

         //投稿ボタンがタップされた時
        } else if (v === sendButton) {
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)
            //★WindowTokenとは

            //val dataBaseReference = FirebaseDatabase.getInstance().reference
            //val genreRef = dataBaseReference.child(ContentsPATH).child(mGenre.toString())

            //val data = HashMap<String, String>()

            // UID ★uidとは UserID
            //data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

            // タイトルと本文を取得する
            val title = titleText.text.toString()
            val body = bodyText.text.toString()

            //タイトルと本文が入力されていることを確認
            if (title.isEmpty()) {
                // タイトルが入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.input_title), Snackbar.LENGTH_LONG).show()
                return
            }

            if (body.isEmpty()) {
                // 質問が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.question_message), Snackbar.LENGTH_LONG).show()
                return
            }

            // Preferenceから名前を取る
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val name = sp.getString(NameKEY, "")

            // FirestoreQuestionのインスタンスを作成し、値を詰めていく
            // FirestoreはCollectionというものに値を詰めていく。
            //collection("contents")でcontentsというリストのようなものを作成している。
            var fireStoreQuestion = FirestoreQuestion()

            fireStoreQuestion.uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            fireStoreQuestion.title = title
            fireStoreQuestion.body = body
            fireStoreQuestion.name = name!!
            fireStoreQuestion.genre = mGenre

            //★ここでは何をしている??
            //data["title"] = title
            //data["body"] = body
            //data["name"] = name!!

            // 添付画像を取得する
            // ★as? とは 安全なキャスト演算子で、キャストに失敗したら null を返す。画像が設定されていない場合にキャストしようとするとアプリが落ちるため、as? を使って、画像がないときは null を返すようにしている。
            val drawable = imageView.drawable as? BitmapDrawable

            // 添付画像が設定されていれば画像を取り出してBASE64エンコードする
            // 画像はBASE64エンコードというデータを文字列に変換する仕組みを使って文字列にする
            //Firebaseは文字列や数字しか保存できませんがこうすることで画像をFirebaseに保存可能となる。
            if (drawable != null) {
                val bitmap = drawable.bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
                fireStoreQuestion.image = bitmapString

                //data["image"] = bitmapString
            }

            //保存する際はDatabaseReferenceクラスのsetValueを使うが、今回は第2引数も指定
            //第2引数にはCompletionListenerクラスを指定 （今回はActivityがCompletionListenerクラスを実装している）
            //画像を保存する可能性があり、保存するのに時間を要することが予想されるのでCompletionListenerクラスで完了を受け取るようにする
            //genreRef.push().setValue(data, this)
            progressBar.visibility = View.VISIBLE

            //★ Firestoreにデータを送信する?
            //document(fireStoreQuestion.id)は、Realmtime Databseのときのkeyを自分で設定している
            //set(fireStoreQuestion)で先に設定したkeyに対しての値を入れている
            FirebaseFirestore.getInstance()
                .collection(ContentsPATH)
                .document(fireStoreQuestion.id)
                .set(fireStoreQuestion)
                    //通信成功時の処理
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    finish()
                }
                    //通信失敗時の処理
                .addOnFailureListener {
                    it.printStackTrace() //★printStackTrace()とは
                    progressBar.visibility = View.GONE
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.question_send_error_message), Snackbar.LENGTH_LONG).show()
                }
        }
    }

    //onRequestPermissionsResult	許可ダイアログでユーザが選択した結果を受け取る。
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { //許可したかどうかを判断
                    // ユーザーが許可したとき
                    showChooser()
                }
                return
            }
        }
    }

    //showChooser
    // ギャラリーから選択するIntentとカメラで撮影するIntentを作成して、さらにそれらを選択するIntentを作成してダイアログを表示させる
    private fun showChooser() {
        // ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        //IntentクラスのcreateChooserメソッドの第1引数に1つ目のIntentを指定し、第2引数にはダイアログへ表示するタイトルを指定
        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.get_image))

        //↑のIntentに対し、
        //chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent})
        //と2つ目のIntentを指定することでこれら2つのIntentを選択するダイアログを表示することができる。
        // EXTRA_INITIAL_INTENTSにカメラ撮影のIntentを追加
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    //onComplete	Firebaseへの保存完了時に呼ばれる。
    //Firebaseへの保存が完了したらfinishメソッドを呼び出してActivityを閉じる。
    // もし失敗した場合はSnackbarでエラーの旨を表示

   /*★もう必要ない??
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.question_send_error_message), Snackbar.LENGTH_LONG).show()
        }
    }
    */
}