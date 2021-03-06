package com.milburn.mytlc;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private List<Shift> mShiftArray = new ArrayList<>();
    private List<String> mExpandPos = new ArrayList<>();
    private Context context;
    private LayoutInflater layoutInflater;
    private PrefManager pm;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextViewDay;
        private TextView mTextViewDayNum;
        private TextView mTextViewHours;
        private TextView mTextViewTime1;
        private TextView mTextViewDept1;
        private TextView mTextViewActivity;

        private ImageView imageArrow;
        private ConstraintLayout constraintExtra;
        private LinearLayout linearLayout;

        private TextView mTextViewSpan;
        private TextView mTextViewWeekHours;
        private TextView mTextViewWeekPay;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextViewDay = (TextView)itemView.findViewById(R.id.textview_day_name);
            mTextViewDayNum = (TextView)itemView.findViewById(R.id.textview_day_num);
            mTextViewHours = (TextView)itemView.findViewById(R.id.textview_hours_num);
            mTextViewTime1 = (TextView)itemView.findViewById(R.id.textview_time);
            mTextViewDept1 = (TextView)itemView.findViewById(R.id.textview_dept_1);
            mTextViewActivity = (TextView)itemView.findViewById(R.id.textview_activity);

            imageArrow = (ImageView)itemView.findViewById(R.id.image_drop_down);
            constraintExtra = (ConstraintLayout)itemView.findViewById(R.id.constraintlayout_extra);
            linearLayout = (LinearLayout)itemView.findViewById(R.id.linearlayout);

            mTextViewSpan = (TextView)itemView.findViewById(R.id.textview_dates);
            mTextViewWeekHours = (TextView)itemView.findViewById(R.id.textview_hours);
            mTextViewWeekPay = (TextView)itemView.findViewById(R.id.textview_pay);
        }
    }

    public RecyclerAdapter(List<Shift> itemArray, Context con) {
        pm = new PrefManager(con, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {} });

        if (pm.getDisplay()) {
            Credentials credentials = new Credentials(con);
            ArrayList tempArchiveShifts = new ArrayList();
            tempArchiveShifts.addAll(credentials.getPastSchedule());
            Collections.reverse(tempArchiveShifts);
            mShiftArray.addAll(tempArchiveShifts);
        }
        mShiftArray.addAll(itemArray);
        addDividers();
        context = con;
        context.setTheme(pm.getTheme());
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        if (mShiftArray.get(position) == null) {
            return R.layout.item_cardview_totalweek;
        }
        return R.layout.item_cardview_single;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        if (mShiftArray.get(position) == null) {
            String[] calcs = getTotals(position);
            holder.mTextViewSpan.setText(calcs[1]);
            holder.mTextViewWeekHours.setText(calcs[0]);
            holder.mTextViewWeekPay.setText(calcs[2]);
            return;
        } else {
            final String pos = String.valueOf(position);
            final boolean deptDiff = mShiftArray.get(position).getDeptDiff();
            final boolean actDiff = mShiftArray.get(position).getActDiff();

            holder.mTextViewDay.setText(mShiftArray.get(position).getStartTime("E"));
            holder.mTextViewDayNum.setText(mShiftArray.get(position).getStartTime("d"));
            holder.mTextViewHours.setText(mShiftArray.get(position).getTotalHours().toString());
            holder.mTextViewTime1.setText(mShiftArray.get(position).getCombinedTime());
            holder.mTextViewDept1.setText(mShiftArray.get(position).getDept(0));
            holder.mTextViewActivity.setText(mShiftArray.get(position).getActivity(0));

            holder.constraintExtra.setVisibility(View.GONE);
            holder.imageArrow.setVisibility(View.GONE);

            if (deptDiff || actDiff) {
                holder.linearLayout.removeAllViews();
                int i = -1;
                for (String deptName : mShiftArray.get(position).getDepts()) {
                    i++;
                    View extraFrag = layoutInflater.inflate(R.layout.extra_time_frag, null);
                    holder.linearLayout.addView(extraFrag);

                    TextView time = (TextView)extraFrag.findViewById(R.id.textview_time);
                    time.setText(mShiftArray.get(position).getCombinedTime(i));

                    TextView dept = (TextView)extraFrag.findViewById(R.id.textview_dept);
                    dept.setText(deptName);

                    TextView act = (TextView)extraFrag.findViewById(R.id.textview_activity);
                    act.setText(mShiftArray.get(position).getActivity(i));
                }
            }

            if (mExpandPos.contains(pos) && (deptDiff || actDiff)) {
                holder.constraintExtra.setVisibility(View.VISIBLE);
                holder.imageArrow.setVisibility(View.VISIBLE);
                holder.imageArrow.setRotation(180);
            } else if (deptDiff || actDiff) {
                holder.constraintExtra.setVisibility(View.GONE);
                holder.imageArrow.setVisibility(View.VISIBLE);
                holder.imageArrow.setRotation(0);
            } else {
                holder.imageArrow.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (deptDiff || actDiff) {
                        ConstraintLayout constraintExtra = (ConstraintLayout)v.findViewById(R.id.constraintlayout_extra);
                        if (constraintExtra.getVisibility() == View.VISIBLE) {
                            if (mExpandPos.contains(pos)) {
                                mExpandPos.remove(pos);
                            }
                            constraintExtra.setVisibility(View.GONE);
                        } else {
                            mExpandPos.add(pos);
                            constraintExtra.setVisibility(View.VISIBLE);
                        }
                        notifyItemChanged(position);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mShiftArray.size();
    }

    private void addDividers() {
        List<Shift> tempList = new ArrayList<>();
        tempList.addAll(mShiftArray);
        Date lastDate = null;

        for (Shift shift : mShiftArray) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(shift.getSingleDayDate());
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

            if (lastDate == null) {
                lastDate = cal.getTime();
            }

            if (!cal.getTime().equals(lastDate)) {
                tempList.add(tempList.indexOf(shift), null);
                lastDate = cal.getTime();
            }

        }

        if (tempList.get(tempList.size()-1) != null) {
            tempList.add(tempList.size(), null);
        }

        mShiftArray = tempList;
    }

    private String[] getTotals(Integer position) {
        List<Shift> list = new ArrayList<>();
        int i = position - 1;
        while (i >= 0 && mShiftArray.get(i) != null) {
            list.add(mShiftArray.get(i));
            i--;
        }

        float hours = 0;
        for (Shift shift : list) {
            if (shift != null) {
                hours+= shift.getTotalHours();
            }
        }

        if (!list.isEmpty()) {
            Date dayInWeek = list.get(list.size() - 1).getStartTime();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dayInWeek);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

            Date first = calendar.getTime();
            calendar.add(Calendar.DATE, 6);
            Date last = calendar.getTime();

            SimpleDateFormat dateFormat = new SimpleDateFormat("M'/'d");
            String dates = dateFormat.format(first) + "–" + dateFormat.format(last);
            String totalHours = String.valueOf(hours) + " Hours";

            PrefManager pm = new PrefManager(context, new PrefManager.onPrefChanged() {
                @Override
                public void prefChanged(SharedPreferences sharedPreferences, String s) {
                    //
                }
            });

            String finalPay;
            try {
                Float pay = Float.valueOf(pm.getPay());
                Float tax = Float.valueOf(pm.getTax());
                DecimalFormat df = new DecimalFormat("0.00");
                finalPay = "$" + String.valueOf(df.format((pay * hours) - ((pay * hours) * (tax / 100.0))));
            } catch (Exception e) {
                finalPay = "0";
                Crashlytics.logException(e);
            }

            return new String[]{totalHours, dates, finalPay};
        }
        return new String[]{"0", "0", "0"};
    }

    public Integer getPosition(Shift shift) {
        int i = -1;
        for (Shift shift1 : mShiftArray) {
            i++;
            if (shift1 != null && shift1.equals(shift)) {
                return i;
            }
        }
        return 0;
    }
}
