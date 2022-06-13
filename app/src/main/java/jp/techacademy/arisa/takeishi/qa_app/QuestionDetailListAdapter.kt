package jp.techacademy.arisa.takeishi.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.list_question_detail.view.*


//今まで作成したListViewとはレイアウトファイルを2つ使うという点で異なっている
// 今回の方法を使えば2つだけでなく3つ、4つとリストの各行に異なるレイアウトで表示させることが可能となる

class QuestionDetailListAdapter(context: Context, private val mQuestion: Question) : BaseAdapter() {

    companion object {
        //どのレイアウトを使って表示させるかを判断するためのタイプを表す定数
        //質問と回答の2つのレイアウトを使うので2つの定数を用意
        private val TYPE_QUESTION = 0
        private val TYPE_ANSWER = 1
    }

    private var mLayoutInflater: LayoutInflater? = null

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    //getCount	アイテム（データ）の数を返す
    override fun getCount(): Int {
        return 1 + mQuestion.answers.size
    }

    //引数で渡ってきたポジションがどのタイプかを返す
    override fun getItemViewType(position: Int): Int {
        //1行目、つまりポジションが0の時に質問であるTYPE_QUESTIONを返し、それ以外は回答なのでTYPE_ANSWERを返すようにしている
        return if (position == 0) {
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }

    //★どんな役割？
    override fun getViewTypeCount(): Int {
        return  2
    }

    //getItem	アイテム（データ）を返す
    override fun getItem(position: Int): Any {
        return mQuestion
    }

    //getItemId	アイテム（データ）のIDを返す
    override fun getItemId(position: Int): Long {
        return 0
    }

    //getView	Viewを返す
    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var convertView = view

        //getItemViewTypeメソッドを呼び出してどちらのタイプかを判断してレイアウトファイルを指定
        if (getItemViewType(position) == TYPE_QUESTION) {
            if (convertView == null) {
                convertView = mLayoutInflater!!.inflate(R.layout.list_question_detail, parent, false)!!
            }
            val body = mQuestion.body
            val name = mQuestion.name

            //list_question_detailのbodyTextView
            val bodyTextView = convertView.bodyTextView as TextView
            bodyTextView.text = body

            val nameTextView = convertView.nameTextView as TextView
            nameTextView.text = name

            val bytes = mQuestion.imageBytes
            if (bytes.isNotEmpty()) { //★ARGB_8888はどこから持ってきたもので、何を表すもの？
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                val imageView = convertView.findViewById<View>(R.id.imageView) as ImageView
                imageView.setImageBitmap(image)
            }
        } else {
            if (convertView == null) {
                convertView = mLayoutInflater!!.inflate(R.layout.list_answer, parent, false)!!
            }

            val answer = mQuestion.answers[position - 1] // ★[position - 1]の意味
            val body = answer.body
            val name = answer.name

            val bodyTextView = convertView.bodyTextView as TextView
            bodyTextView.text = body

            val nameTextView = convertView.nameTextView as TextView
            nameTextView.text = name
        }

        return convertView
    }
}