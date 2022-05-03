package kr.co.ajjulcoding.team.project.holo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kr.co.ajjulcoding.team.project.holo.databinding.FragmentUsersettingBinding


class UsersettingFragment(val currentUser:HoloUser) : Fragment() {

    private lateinit var _activity:MainActivity
    private val mActivity get() = _activity
    private lateinit var _binding: FragmentUsersettingBinding
    private val binding get() = _binding
    private val homeViewModel: HomeViewModel by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _activity = requireActivity() as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUsersettingBinding.inflate(inflater, container, false)
        setProfile()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homeViewModel.userLocation.observe(viewLifecycleOwner){
            binding.textLocation.setText(it)
        }
        homeViewModel.userProfile.observe(viewLifecycleOwner){imgUri -> // TODO: 사용자 정보 수정될 때 호출되게
            Glide.with(_activity).load(imgUri).apply {
                RequestOptions()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
            }.into(binding.profilePhoto)
        }
        binding.profilePhoto.setOnClickListener {
            mActivity.changeFragment(AppTag.PROFILE_TAG)
        }
        binding.textScore.setOnClickListener {
            mActivity.supportFragmentManager?.let{fragmentManager ->
                val dialog:ScoreFragment = ScoreFragment(currentUser)
                if (null == fragmentManager.findFragmentByTag(AppTag.SCORE_TAG)) {
                    dialog.show(fragmentManager, AppTag.SCORE_TAG)
                }
            }
        }
        binding.textLocationSet.setOnClickListener {
            mActivity.changeFragment(AppTag.GPS_TAG)
        }
        binding.textUtilityBill.setOnClickListener {
            mActivity.supportFragmentManager?.let{fragmentManager ->
                val dialog:UtilityBillFragment = UtilityBillFragment()
                if (null == fragmentManager.findFragmentByTag(AppTag.UTILITYBILLDIALOG_TAG)) {
                    dialog.show(fragmentManager, AppTag.UTILITYBILLDIALOG_TAG)
                }
            }
        }
        binding.textAccount.setOnClickListener {
            mActivity.changeFragment(AppTag.ACCOUNT_TAG)
        }
        binding.textLogout.setOnClickListener {
            SettingInApp.mAuth.signOut()
            Toast.makeText(requireActivity(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            mActivity.changetoLoginActivity()
        }
        binding.textWithdrawal.setOnClickListener {
            mActivity.supportFragmentManager?.let{fragmentManager ->
                val dialog:WithdrawalDialogFragment = WithdrawalDialogFragment()
                if (null == fragmentManager.findFragmentByTag(AppTag.WITHDRAWALDIALOG_TAG)) {
                    dialog.show(fragmentManager, "CustomDialog")
                }
            }
        }
    }

    private fun setProfile(){
        Log.d("실행", "onRequestPermissionsResult() _ 권한 허용")
        binding.textNickname.setText(currentUser.nickName)
        if (currentUser.location == null){
            binding.textLocation.setText("위치 미설정")
        }else
            binding.textLocation.setText(currentUser.location)
            binding.textEmail.setText(currentUser.uid)
        currentUser.profileImg?.let {
            Glide.with(_activity).load(Uri.parse(it)).apply {
                RequestOptions()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
            }.into(binding.profilePhoto)
        }
    }
}