package com.seuprojeto;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContentFragment extends Fragment implements ChannelAdapter.OnChannelClickListener {
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private List<Channel> contentList;

    // Método de fábrica para criar o fragment com a lista de conteúdo
    public static ContentFragment newInstance(List<Channel> channels) {
        ContentFragment fragment = new ContentFragment();
        Bundle args = new Bundle();
        args.putSerializable("content_list", (Serializable) channels);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            contentList = (List<Channel>) getArguments().getSerializable("content_list");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false); // Crie um layout simples para ele
        recyclerView = view.findViewById(R.id.contentRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        processAndDisplayChannels(contentList);
        return view;
    }
    
    // Lógica movida da ChannelListActivity
    private void processAndDisplayChannels(List<Channel> channels) {
        Map<String, List<Channel>> channelsByGroup = new LinkedHashMap<>();
        for (Channel channel : channels) {
            String group = channel.getGroup();
            if (group == null || group.trim().isEmpty()) group = "Outros";
            List<Channel> groupList = channelsByGroup.get(group);
            if (groupList == null) {
                groupList = new ArrayList<>();
                channelsByGroup.put(group, groupList);
            }
            groupList.add(channel);
        }

        List<ChannelCategory> categories = new ArrayList<>();
        for (Map.Entry<String, List<Channel>> entry : channelsByGroup.entrySet()) {
            categories.add(new ChannelCategory(entry.getKey(), entry.getValue()));
        }

        categoryAdapter = new CategoryAdapter(categories, this);
        recyclerView.setAdapter(categoryAdapter);
    }

    @Override
    public void onChannelClick(Channel channel) {
        // ... (cole aqui a lógica onChannelClick da sua ChannelListActivity)
        ArrayList<Channel> listToSend = new ArrayList<>();
        for(ChannelCategory category : categoryAdapter.getCategoryList()){
            if(category.getChannels().contains(channel)){
                listToSend.addAll(category.getChannels());
                break;
            }
        }
        int clickedIndex = listToSend.indexOf(channel);
        if(clickedIndex != -1) {
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra("channel_list", listToSend);
            intent.putExtra("start_position", clickedIndex);
            startActivity(intent);
        }
    }
}