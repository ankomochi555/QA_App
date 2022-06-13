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
import kotlinx.android.synthetic.main.list_questions.view.*

class FavoriteListAdapter (context: Context) : BaseAdapter() {
    private var mLayoutInflater: LayoutInflater
    private var mFavoriteArrayList = ArrayList<Question>() //質問一覧,項目

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    //getCount	アイテム（データ）の数を返す
    override fun getCount(): Int {
        return mFavoriteArrayList.size
    }

    //getItem	アイテム（データ）を返す
    override fun getItem(position: Int): Any {
        return mFavoriteArrayList[position]
    }

    //getItemId	アイテム（データ）のIDを返す
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    //getView	Viewを返す
    override fun getView(position: Int, view: View?, parent: ViewGroup?): View {
        var convertView = view

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.list_favorite, parent, false)
        }

        val titleText = convertView!!.titleTextView as TextView
        titleText.text = mFavoriteArrayList[position].name

        val nameText = convertView.nameTextView as TextView
        nameText.text = mFavoriteArrayList[position].name

        val resText = convertView.resTextView as TextView
        val resNum = mFavoriteArrayList[position].answers.size
        resText.text = resNum.toString()

        //ImageViewに設定するBitmapはbyteの配列から生成
        val bytes = mFavoriteArrayList[position].imageBytes
        if (bytes.isNotEmpty()) {//★Configとは「設定」のこと。 もしくは「設定ファイル」のこと
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
            val imageView = convertView.imageView as ImageView
            imageView.setImageBitmap(image)
        }

        return convertView
    }

    fun setFavoriteArrayList(questionArrayList: ArrayList<Question>) {
        mFavoriteArrayList = questionArrayList
    }
}