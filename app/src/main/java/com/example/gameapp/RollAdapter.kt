package com.example.gameapp

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.gameapp.databinding.RecyclerviewItemBinding
import com.example.gameapp.model.Roll
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

open class RollAdapter(query: Query) :
    FirestoreAdapter<RollAdapter.ViewHolder>(query) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RecyclerviewItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getSnapshot(position))
    }

    class ViewHolder(private val binding: RecyclerviewItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(snapshot: DocumentSnapshot) {
            // Current roll
            val roll = snapshot.toObject<Roll>() ?: return

            // Get imageviews from the recyclerview item
            val images : ArrayList<ImageView> = ArrayList()
            images.add(binding.firstRoll)
            images.add(binding.secondRoll)
            images.add(binding.thirdRoll)
            images.add(binding.fourthRoll)
            images.add(binding.fifthRoll)
            images.add(binding.sixthRoll)

            // Get images of the dices from resources
            val resources : ArrayList<Int> = ArrayList()
            resources.add(R.drawable.one)
            resources.add(R.drawable.two)
            resources.add(R.drawable.three)
            resources.add(R.drawable.four)
            resources.add(R.drawable.five)
            resources.add(R.drawable.six)

            // If the roll exists
            if (roll != null) {
                binding.textView.contentDescription = snapshot.id

                // Init imageviews
                binding.firstRoll.visibility = View.GONE
                binding.secondRoll.visibility = View.GONE
                binding.thirdRoll.visibility = View.GONE
                binding.fourthRoll.visibility = View.GONE
                binding.fifthRoll.visibility = View.GONE
                binding.sixthRoll.visibility = View.GONE

                // Make necessary images visible and add correct dice image into it
                for (i in roll.numbers.indices) {
                    val num = roll.numbers[i]
                    images[i].setImageResource(resources[num - 1])
                    images[i].visibility = View.VISIBLE

                    val newWidth = 110
                    val newHeight = 110
                    val params = images[i].layoutParams
                    params.width = newWidth
                    params.height = newHeight

                    images[i].layoutParams = params
                }
            }
        }
    }
}
