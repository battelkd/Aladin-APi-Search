package com.jxino.aladinaccessiblebookapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jxino.aladinaccessiblebookapp.data.BookRepository
import com.jxino.aladinaccessiblebookapp.domain.BookSearchEnhancer
import com.jxino.aladinaccessiblebookapp.domain.PassthroughBookSearchEnhancer
import com.jxino.aladinaccessiblebookapp.domain.ResultAnnouncer
import com.jxino.aladinaccessiblebookapp.domain.UserUtteranceParser

class BookSearchViewModelFactory(
    private val repository: BookRepository,
    private val parser: UserUtteranceParser,
    private val announcer: ResultAnnouncer,
    private val enhancer: BookSearchEnhancer = PassthroughBookSearchEnhancer(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BookSearchViewModel(repository, parser, announcer, enhancer) as T
    }
}
