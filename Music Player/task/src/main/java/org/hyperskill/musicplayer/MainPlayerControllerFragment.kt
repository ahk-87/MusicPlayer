package org.hyperskill.musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.hyperskill.musicplayer.databinding.FragmentMainPlayerControllerBinding


class MainPlayerControllerFragment : Fragment() {

    private var _binding: FragmentMainPlayerControllerBinding? = null
    private val binding get() = _binding!!
    private lateinit var activity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity
        // Inflate the layout for this fragment
        _binding = FragmentMainPlayerControllerBinding.inflate(inflater, container, false)

        activity.mediaPlayer.registerControllers(
            binding.controllerSeekBar,
            binding.controllerTvCurrentTime,
            binding.controllerTvTotalTime
        )

        binding.controllerBtnPlayPause.setOnClickListener {
            activity.mediaPlayer.playOrPause()
        }

        binding.controllerBtnStop.setOnClickListener {
            activity.mediaPlayer.stop()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.scroll()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.mediaPlayer.unRegisterControllers()
        _binding = null
    }
}