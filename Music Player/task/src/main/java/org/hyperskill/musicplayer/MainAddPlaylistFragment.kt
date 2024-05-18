package org.hyperskill.musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.hyperskill.musicplayer.databinding.FragmentMainAddPlaylistBinding

class MainAddPlaylistFragment : Fragment() {

    private var _binding: FragmentMainAddPlaylistBinding? = null
    private val binding get() = _binding!!
    lateinit var activity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as MainActivity
        // Inflate the layout for this fragment
        _binding = FragmentMainAddPlaylistBinding.inflate(inflater, container, false)

        binding.addPlaylistBtnOk.setOnClickListener { addPlaylist() }
        binding.addPlaylistBtnCancel.setOnClickListener { cancelAdding() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.scroll()
    }

    private fun addPlaylist() {
        val playlistName = binding.addPlaylistEtPlaylistName.text.toString()
        activity.addNewPlaylist(playlistName)
    }

    private fun cancelAdding() {
        activity.exitAddingPlaylist()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}