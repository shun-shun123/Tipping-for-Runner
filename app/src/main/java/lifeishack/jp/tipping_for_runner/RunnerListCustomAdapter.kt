package lifeishack.jp.tipping_for_runner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.custom_layout.view.*

class RunnerListCustomAdapter(context: Context, objects: MutableList<CustomListData>): ArrayAdapter<CustomListData>(context, 0, objects) {

    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        var holder: RunnerListViewHolder

        if (view == null) {
            view = layoutInflater.inflate(R.layout.custom_layout, parent, false) ?: null
            holder = RunnerListViewHolder(
                    view?.runner_number!!,
                    view.marathon_name,
                    view.runner_name)
            view.tag = holder
        } else {
            holder = view.tag as RunnerListViewHolder
        }

        val runner = getItem(position) as CustomListData
        holder.runnerNumber.text = runner.runnerNumber
        holder.marathonName.text = runner.marathonName
        holder.runnerName.text = runner.runnerName

        return view
    }

}