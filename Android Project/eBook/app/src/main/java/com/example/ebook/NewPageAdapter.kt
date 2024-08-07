package com.example.ebook

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class NewPageAdapter(context: Context, private val pages: List<NewPage>) :
    ArrayAdapter<NewPage>(context, 0, pages) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val page = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = page?.text
        return view
    }
}
