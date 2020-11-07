package com.example.duviservicios.Callback

import com.example.duviservicios.Model.CommentModel

interface ICommentCallBack {
    fun onCommentLoadSuccess(commentList:List<CommentModel>)
    fun onCommentLoadFailed(messaje:String)
}