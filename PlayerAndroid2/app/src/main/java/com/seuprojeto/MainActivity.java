package com.seuprojeto;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            // No futuro, podemos fazer os outros botões carregarem listas específicas
            // mas a arquitetura de carregar sob demanda será mantida.

            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Por enquanto, apenas o botão Home é o principal
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else {
                // Podemos adicionar a lógica para os outros botões depois
                // Temporariamente, eles podem carregar a Home também ou mostrar uma mensagem.
                selectedFragment = new HomeFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });

        // Carrega o fragment inicial
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.commit();
    }
}