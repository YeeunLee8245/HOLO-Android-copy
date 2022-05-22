package kr.co.ajjulcoding.team.project.holo

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kr.co.ajjulcoding.team.project.holo.databinding.ActivityMainBinding
import java.io.File
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var _binding:ActivityMainBinding
    private val binding get() = _binding
    private lateinit var homeFragment:HomeFragment
    private lateinit var profileFragment:ProfileFragment
    private lateinit var accountFragment:AccountFragment
    private lateinit var userSettingFragment:UsersettingFragment
    private val withdrawalDialogFragment = WithdrawalDialogFragment()
    private lateinit var utilityBillFragment:UtilityBillFragment
    private val notificationFragment = NotificationFragment()
    private lateinit var scoreFragment:ScoreFragment
    private lateinit var chatListFragment:Fragment
    private lateinit var mUserInfo:HoloUser
    private val gpsFragment = GpsFragment()
    private var currentTag:String = AppTag.HOME_TAG
    private lateinit var frgDic:HashMap<String, Fragment>
    private lateinit var dialog: DialogFragment
    private var waitTime = 0L // 백버튼 2번 시간 간격
    private lateinit var imgLauncher: ActivityResultLauncher<Intent>
    private lateinit var utilitylist:ArrayList<UtilityBillItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        mUserInfo = intent.getParcelableExtra<HoloUser>(AppTag.USER_INFO)!!
        Log.d("유저 데이터 정보", mUserInfo.toString())
        supportFragmentManager.fragmentFactory = ChatListFragmentFactory(mUserInfo)
        super.onCreate(savedInstanceState)
        profileFragment = ProfileFragment(mUserInfo)
        userSettingFragment = UsersettingFragment((mUserInfo))
        scoreFragment = ScoreFragment(mUserInfo)
        accountFragment = AccountFragment(mUserInfo)
        utilityBillFragment = UtilityBillFragment(mUserInfo)
        chatListFragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,ChatListFragment::class.java.name)
        imgLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val uri: Uri? = result.data?.data
            Log.d("사진 가져오기0", "${uri}")
            uri?.let { // 사진 정상적으로 가져옴
                profileFragment.setProfileImg(it)
            }
        }

        showHomeFragment(mUserInfo)
        frgDic = hashMapOf<String, Fragment>(AppTag.PROFILE_TAG to profileFragment,
            AppTag.GPS_TAG to gpsFragment, AppTag.SETTING_TAG to userSettingFragment,
            AppTag.WITHDRAWALDIALOG_TAG to withdrawalDialogFragment,
            AppTag.UTILITYBILLDIALOG_TAG to utilityBillFragment,
            AppTag.SCORE_TAG to scoreFragment, AppTag.ACCOUNT_TAG to accountFragment,
            AppTag.CHATLIST_TAG to chatListFragment, AppTag.NOTIFICATION_TAG to notificationFragment)

        val code = intent.getStringExtra("first")
        if (code == "first") {
            dialog = UtilityBillFragment(mUserInfo)
            dialog.show(supportFragmentManager, "CustomDialog")
        }

        if (intent.getBooleanExtra(AppTag.LOGIN_TAG, false)) {
            CoroutineScope(Dispatchers.Main).launch {
                setProfileImg("profile_" + mUserInfo.uid!!.replace(".", "") + ".jpg")
            }
            saveCache()
        }else if (intent.getBooleanExtra(AppTag.REGISTER_TAG, false)){
            saveCache()
        }

        binding.navigationBar.setOnItemSelectedListener { item ->
            Log.d("프래그먼트 변경 요청", currentTag.toString())
            when (item.itemId) {
                R.id.menu_home -> {
                    changeFragment(AppTag.HOME_TAG)
                }
                R.id.menu_chatting -> {
                    Log.d("프래그먼트 변경 요청", currentTag.toString())
                    changeFragment(AppTag.CHATLIST_TAG)
                }
                R.id.menu_profile -> {
                    Log.d("프래그먼트 변경 요청", currentTag.toString())
                    changeFragment(AppTag.SETTING_TAG)
                }
            }
            true
        }
    }

    override fun onBackPressed() {
        if (currentTag == AppTag.PROFILE_TAG || currentTag == AppTag.GPS_TAG ||
            currentTag == AppTag.ACCOUNT_TAG) {
            changeFragment(AppTag.SETTING_TAG)
        }else if (currentTag == AppTag.NOTIFICATION_TAG || currentTag.contains(WebUrl.URL_LAN))
            changeFragment(AppTag.HOME_TAG)
        else if (System.currentTimeMillis() - waitTime >= 1500){    // 1.5초
            waitTime = System.currentTimeMillis()
            Toast.makeText(this,"뒤로가기 버튼을 한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show()
        }else
            super.onBackPressed()

    }

    fun changeFragment(frgTAG: String){
        val tran = supportFragmentManager.beginTransaction()

        if (currentTag != frgTAG){
            currentTag = frgTAG
            if (AppTag.HOME_TAG == currentTag)
                tran.replace(R.id.fragmentView, homeFragment)   // (스택에 있는)이전 프래그먼트 전부 제거 => TODO: 채팅방 생성시 다이얼로그 aysc 받아오는 걸로 고치기
            else if(currentTag == AppTag.SCORE_TAG) {
                dialog = scoreFragment
                dialog.show(supportFragmentManager, "CustomDialog")
            }
            else if (currentTag.contains(WebUrl.URL_LAN)) {
                Log.d("웹뷰","들어옴")
                tran.add(R.id.fragmentView, WebViewFragment(frgTAG, mUserInfo))    // TODO: replace로 고쳐서 테스트해보기
            }else {
                frgDic[currentTag]!!.let { tran.replace(R.id.fragmentView, it) }    // add로 바꾸면 gps 적용시 강종
            }
            tran.commit()
        }
    }

    fun changetoLoginActivity() {
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        editor.clear()
        editor.commit()
        val intent = Intent(this, LoginActivity::class.java)  // 인텐트를 생성해줌,
        startActivity(intent)  // 화면 전환을 시켜줌
        finish()
    }

    fun withdrawalUser() {
        val repository = Repository()

        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        editor.clear()
        editor.commit()

        val intentLogin = Intent(this, LoginActivity::class.java)
        SettingInApp.uniqueActivity(intentLogin)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intentLogin.flags =  // 탈퇴시 기존 스택 모두 비우고 로그인화면 생성"
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP //액티비티 스택제거
        CoroutineScope(Dispatchers.Main).launch {
            val result = repository.deleteUserInfo(AppTag.currentUserEmail()!!)

            Log.d("탈퇴 데이터 확인", result.toString())
            if (result != false) {
                intentLogin.putExtra(AppTag.currentUserEmail(), false)
                intentLogin.putExtra(AppTag.LOGIN_TAG, false)
                startActivity(intentLogin)
            }else Toast.makeText(this@MainActivity,"서버 통신 오류",Toast.LENGTH_SHORT).show()
        }

    }

    fun setLocationToHome(location:String){
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        userSettingFragment.setUserLocation(location)
        editor.putString("location",location).apply()   // save location
    }

    fun setAccount(account:String){
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        userSettingFragment.setUserAccount(account)
        editor.putString("account",account).apply()   // save account
    }

    suspend fun setProfileImg(fileName:String){
        val dir = File(Environment.DIRECTORY_PICTURES + "/profile_img")
        if (!dir.isDirectory()){
            dir.mkdir()    // 가져온 이미지 저장할 디렉토리 만들기
        }
        CoroutineScope(Dispatchers.IO).async {
            downloadImg(fileName)
        }.await()
    }

    private fun downloadImg(fileName: String){
        val FBstorage = FirebaseStorage.getInstance()
        val FBstorageRef = FBstorage.reference
        FBstorageRef.child("profile_img/"+fileName).downloadUrl
            .addOnSuccessListener { imgUri ->
                Log.d("저장한 프로필 url", imgUri.toString())
                if(currentTag == AppTag.HOME_TAG) {
                    Glide.with(this).load(imgUri).apply { // TODO: 보일러 코드 고치기
                        RequestOptions()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(findViewById(R.id.circleImageView))
                }else if (currentTag == AppTag.SETTING_TAG){
                    Glide.with(this).load(imgUri).apply {
                        RequestOptions()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                    }.into(findViewById(R.id.profilePhoto))
                }
                //Toast.makeText(this, "프로필 이미지 변경 완료!",Toast.LENGTH_SHORT).show()
                userSettingFragment.setUserProfile(imgUri.toString())
                sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)  // 캐시 저장
                editor = sharedPref.edit()
                editor.putString("profile",imgUri.toString()).apply()
            }

    }

    fun storeUtilityCache(mUtilityBillItems: ArrayList<UtilityBillItem>) {
        mUserInfo.utilitylist = mUtilityBillItems
        utilitylist=mUtilityBillItems
        Log.d("메인액티비티 공과금 list count", utilitylist.size.toString())
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        val gson = Gson()
        val json = gson.toJson(utilitylist)
        editor.putString(AppTag.BILLCACHE_TAG, json)
        Log.d("메인액티비티 공과금 json", json)
        editor.apply()
    }

    fun getUtilityJSON(): ArrayList<UtilityBillItem> {
        val type: Type = object : TypeToken<ArrayList<UtilityBillItem?>?>() {}.getType()
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        val gson = Gson()
        val json = sharedPref.getString(AppTag.BILLCACHE_TAG, "")
        utilitylist = gson.fromJson(json, type)

        return utilitylist
    }
    
    private fun saveCache(){
        val userInfo = intent.getParcelableExtra<HoloUser>(AppTag.USER_INFO) as HoloUser
        sharedPref = this.getSharedPreferences(AppTag.USER_INFO,0)
        editor = sharedPref.edit()
        editor.putString("uid", userInfo.uid).apply()
        editor.putString("realName",userInfo.realName).apply()
        editor.putString("nickName",userInfo.nickName).apply()
        editor.putString("score", userInfo.score).apply()
        editor.putString("token", userInfo.token).apply()
        editor.putBoolean("msgValid", userInfo.msgVaild).apply()
    }

    private fun showHomeFragment(userInfo:HoloUser){
        val tran = supportFragmentManager.beginTransaction()
        homeFragment = HomeFragment(userInfo)
        tran.replace(R.id.fragmentView, homeFragment)
        tran.commit()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun showAlertDialog(msg:String, vararg option:String){
        AlertDialog.Builder(this)
            .setTitle(msg)
            .setItems(option, object : DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface, idx: Int) {
                    dialog.dismiss()
                }
            })
            .create().show()
    }
    
    fun getImgCallback() = imgLauncher

    fun addAlarm(position: Int, term: Int, day: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val code = (position.toString()+term.toString()+day.toString()).toInt()

        val intent = Intent(this, Alarm::class.java)
        intent.putExtra("requestCode", code.toString())
        val pendingIntent = PendingIntent.getBroadcast(
            this, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("공과금 mainactivity", position.toString())

        val toastMessage = if (true) {
            val cal = Calendar.getInstance()
            val month = (cal.get(Calendar.MONTH))
            val year = (cal.get(Calendar.YEAR))

            for (i in year until year+10) {
                if (term==0) {
                    for (j in 0 until 12) {
                        setDateFormat(alarmManager, i, j, day, pendingIntent)
                    }
                }
                else if(term==1) {
                    if (month == 0 || month%2 == 0) {
                        for (j in 0 until 12 step(2)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else {
                        for (j in 1 until 12 step(2)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                }
                else {
                    if (month == 0 || month%4 == 0) {
                        for (j in 0 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else if (month%4 == 1) {
                        for (j in 1 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else if (month%4 == 2) {
                        for (j in 2 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                    else {
                        for (j in 2 until 12 step(4)) {
                            setDateFormat(alarmManager, i, j, day, pendingIntent)
                        }
                    }
                }
            }
            "알림이 설정되었습니다."
        } else {
            alarmManager.cancel(pendingIntent)
            "알림이 설정되지 않았습니다."
        }
        Log.d(Alarm.TAG, toastMessage)
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()

    }

    private fun setDateFormat(
        alarmManager: AlarmManager,
        year: Int,
        month: Int,
        day: Int,
        pendingIntent: PendingIntent
    ) {

        val calendar: Calendar = Calendar.getInstance().apply { // 1
            timeInMillis = System.currentTimeMillis()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            Log.d("설정 알람", year.toString()+"년"+month.toString()+"월"+day.toString())
        }

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun delAlarm(position: Int, term: Int, day: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val code = (position.toString()+term.toString()+day.toString()).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            this, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if(pendingIntent!=null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
