package com.finance.loanmanager.ui.data;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.finance.loanmanager.R;
import com.finance.loanmanager.data.AppDatabase;
import com.finance.loanmanager.data.entity.Loan;
import com.finance.loanmanager.data.entity.Payment;
import com.finance.loanmanager.repository.LoanRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class DataManagementActivity extends AppCompatActivity {
    
    private LoanRepository repository;
    private Gson gson;
    
    // 使用新的 Activity Result API
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        repository = new LoanRepository(getApplication());
        gson = new GsonBuilder().setPrettyPrinting().create();
        
        // 注册 Activity Result Launchers
        exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportData(uri);
                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
            }
        );
        
        importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        confirmImport(uri);
                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
            }
        );
        
        String action = getIntent().getAction();
        if ("EXPORT".equals(action)) {
            startExport();
        } else if ("IMPORT".equals(action)) {
            startImport();
        } else {
            finish();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "loan_data_" + System.currentTimeMillis() + ".json");
        exportLauncher.launch(intent);
    }
    
    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importLauncher.launch(intent);
    }
    
    private void exportData(Uri uri) {
        try {
            List<Loan> loans = repository.getAllLoansSync();
            List<Payment> payments = repository.getAllPayments().getValue();
            
            ExportData exportData = new ExportData(loans, payments);
            String json = gson.toJson(exportData);
            
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(json.getBytes());
                outputStream.close();
                Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.export_failed + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        finish();
    }
    
    private void confirmImport(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_data)
                .setMessage(R.string.import_confirm)
                .setPositiveButton(R.string.confirm, (dialog, which) -> importData(uri))
                .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
                .show();
    }
    
    private void importData(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder jsonBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();
                inputStream.close();
                
                String json = jsonBuilder.toString();
                ExportData exportData = gson.fromJson(json, new TypeToken<ExportData>(){}.getType());
                
                if (exportData != null) {
                    // 清空现有数据
                    repository.deleteAllPayments();
                    repository.deleteAllLoans();
                    
                    // 导入新数据
                    if (exportData.loans != null) {
                        for (Loan loan : exportData.loans) {
                            loan.setId(0); // 重置ID
                            repository.insertLoan(loan);
                        }
                    }
                    
                    if (exportData.payments != null) {
                        for (Payment payment : exportData.payments) {
                            payment.setId(0); // 重置ID
                            repository.insertPayment(payment);
                        }
                    }
                    
                    Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.import_failed + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        finish();
    }
    
    private static class ExportData {
        List<Loan> loans;
        List<Payment> payments;
        
        ExportData(List<Loan> loans, List<Payment> payments) {
            this.loans = loans;
            this.payments = payments;
        }
    }
}
