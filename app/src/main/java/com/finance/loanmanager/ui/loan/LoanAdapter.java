package com.finance.loanmanager.ui.loan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finance.loanmanager.R;
import com.finance.loanmanager.repository.LoanRepository;
import com.finance.loanmanager.util.NumberFormatUtil;

import java.util.List;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.ViewHolder> {

    private List<LoanRepository.LoanWithStatus> loans;
    private OnLoanClickListener listener;

    public interface OnLoanClickListener {
        void onLoanClick(LoanRepository.LoanWithStatus loan);
    }

    public LoanAdapter(List<LoanRepository.LoanWithStatus> loans, OnLoanClickListener listener) {
        this.loans = loans;
        this.listener = listener;
    }

    public void updateData(List<LoanRepository.LoanWithStatus> loans) {
        this.loans = loans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_loan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoanRepository.LoanWithStatus item = loans.get(position);
        holder.tvName.setText(item.loan.getName());
        holder.tvRemaining.setText("剩余: " + NumberFormatUtil.formatCurrency(item.status.getRemainingPrincipal()));
        holder.tvMonthlyPayment.setText("月供: " + NumberFormatUtil.formatCurrency(item.status.getNewMonthlyPayment()));
        holder.tvIcon.setText(item.loan.isCreditCard() ? "💳" : "🏦");
        
        if (item.status.isPaidOff()) {
            holder.tvName.append(" ✓");
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLoanClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return loans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon;
        TextView tvName;
        TextView tvRemaining;
        TextView tvMonthlyPayment;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvMonthlyPayment = itemView.findViewById(R.id.tvMonthlyPayment);
        }
    }
}
