package memoque.bobs.com.memoque.main

import android.annotation.SuppressLint
import android.content.Context
import memoque.bobs.com.memoque.db.DBManager
import memoque.bobs.com.memoque.main.adapter.IAdapter
import memoque.bobs.com.memoque.main.memo.BSMemo
import memoque.bobs.com.memoque.notification.NotificationInfo
import org.joda.time.DateTime
import java.util.*

class MemoQueManager private constructor() {

    enum class Adapterkey {
        MEMO, SEARCH
    }

    private var databaseManager: DBManager? = null
    private var memos = mutableListOf<BSMemo>()
    private val adapterListeners = hashMapOf<Adapterkey, IAdapter>()

    companion object {
        @SuppressLint("StaticFieldLeak")
        val instance = MemoQueManager()
        val MAX_MEMO = 500
    }

    fun setDatabase(context: Context) {
        // 디비를 세팅하고 디비에 저장되어있는 메모리스트를 가져온다
        databaseManager = DBManager(context)
        memos = databaseManager!!.allMemos
    }

    fun setAdapterListener(key: Adapterkey, listener: IAdapter) {
        // 탭별 리사이클러뷰 어뎁터를 세팅한다
        adapterListeners[key] = listener
    }

    fun add(key: Adapterkey, BSMemo: BSMemo) {
        // 메모 추가
        memos.add(BSMemo)
        adapterListeners[key]?.refreshAll()
        databaseManager?.insert(BSMemo)
    }

    fun update(key: MemoQueManager.Adapterkey, BSMemo: BSMemo) {
        // 메모 업데이트
        when (key) {
            Adapterkey.MEMO -> adapterListeners[key]?.refreshAll()
            Adapterkey.SEARCH -> {
                adapterListeners[Adapterkey.MEMO]?.refreshAll()
                adapterListeners[key]?.refreshAll()
            }
        }

        databaseManager?.update(BSMemo)
    }

    fun remove(key: MemoQueManager.Adapterkey, memoindex: Int): Boolean {
        var removeMemo = BSMemo()

        for (memo in memos) {
            if (memo.index == memoindex) {
                removeMemo = memo
            }
        }

        memos.remove(removeMemo)

        // 메모 삭제
        databaseManager?.delete(removeMemo)

        when (key) {
            Adapterkey.MEMO ->
                adapterListeners[key]?.refreshAll()
            Adapterkey.SEARCH -> {
                adapterListeners[Adapterkey.MEMO]?.refreshAll()
                adapterListeners[key]?.refreshAll()
            }
        }

        return true
    }

    fun memosSearch(key: MemoQueManager.Adapterkey, filterText: String): Boolean {
        // 메모 검색
        if (memos.size == 0)
            return false
        else {
            listSort()
            val flitermemos = filterMemos(filterText)
            if (flitermemos.isEmpty())
                return false
            else
                adapterListeners[key]?.searchMemos(flitermemos, filterText)
        }

        return true
    }

    fun filterMemos(filterText: String): List<BSMemo> {
        return memos.filter { it.title.contains(filterText) || it.content.contains(filterText) || it.date.contains(filterText) }
    }

    fun initAdapterToTab(key: Adapterkey) {
        adapterListeners[key]?.clear()
    }

    private fun listSort() {
        // 메모 리스트를 인덱스값 기준으로 오름차순 정렬한다
        if (memos.isEmpty())
            return

        Collections.sort(memos, Comparator { o1, o2 ->
            if (o1.index < o2.index)
                return@Comparator -1
            else if (o1.index > o2.index)
                return@Comparator 1
            0
        })
    }

    fun getMemoToMemoIndex(index: Int): BSMemo? {
        for (memo in memos) {
            if (memo.index == index) return memo
        }

        return null
    }

    fun getMemoIndex(): Int {
        // 다음 메모 인덱스를 리턴해야하므로 마지막 메모의 인덱스 +1 값을 리턴한다
        var index = 0

        if (memos.size > 0) {
            listSort()
            index = (memos[memos.size - 1].index + 1)
        }

        return index
    }

    fun getMemos(): MutableList<BSMemo> {
        // 메모리스트 리턴
        listSort()
        return memos
    }

    fun getMemosSize(): Int {
        // 메모리스트 크기 리턴
        return memos.size
    }

    fun checkMemosDate() {
        val now = DateTime()

        for (memo in memos) {
            if (memo.dateTime!!.isBefore(now))
                memo.isCompleteNoti = true
        }
    }

    fun isMemosNotNoti(): Boolean {
        var notNoti = true

        if (memos.size > 0) {
            for (memo in memos) {
                if (!memo.isCompleteNoti)
                    notNoti = false
            }
        }

        return notNoti
    }

    fun isMaxMemos(): Boolean {
        return memos.size == MAX_MEMO
    }

    fun getNextNotiInfo(): NotificationInfo {
        val notificationInfo = NotificationInfo()
        val now = DateTime()

        val dateComparatorList = memos
        Collections.sort(dateComparatorList, dataComparator)

        for (i in 0 until dateComparatorList.size) {
            val memo = dateComparatorList[i]

            val minMilliseconds = memo.dateTime!!.millis - now.millis

            if (minMilliseconds < 0 || memo.isCompleteNoti)
                continue

            notificationInfo.setInfoData(memo, minMilliseconds)
            break
        }

        return notificationInfo
    }

    val dataComparator = Comparator { o1: BSMemo, o2: BSMemo ->

        val now = DateTime()

        val o1Millisecond = o1.dateTime!!.millis - now.millis
        val o2Millisecond = o2.dateTime!!.millis - now.millis

        if (o1Millisecond < o2Millisecond)
            return@Comparator -1
        else if (o1Millisecond > o2Millisecond)
            return@Comparator 1
        return@Comparator 0
    }
}
