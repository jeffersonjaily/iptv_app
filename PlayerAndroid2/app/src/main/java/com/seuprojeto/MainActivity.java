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
            // A lógica de filtro para Filmes/Séries/Ao Vivo precisará ser adaptada
            // para carregar uma lista específica ou filtrar após o carregamento.
            // Por enquanto, vamos focar em fazer a Home funcionar perfeitamente.

            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else {
                // Para os outros botões, podemos criar um HomeFragment com uma URL de lista diferente
                // ou implementar a lógica de filtro na próxima tela.
                // Por enquanto, eles podem recarregar a home ou não fazer nada.
                selectedFragment = new HomeFragment(); // Temporariamente, todos carregam a home
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