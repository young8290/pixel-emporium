package com.example.business.controller;

import com.example.business.response.BrowseHistoryResponse;
import com.example.business.response.ProductResponse;
import com.example.business.service.BrowseHistoryService;
import com.example.business.service.ProductFavoriteService;
import com.example.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductEngagementControllerTest {
    @Mock
    private ProductFavoriteService favoriteService;
    @Mock
    private BrowseHistoryService historyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProductFavoriteController(favoriteService),
                new BrowseHistoryController(historyService))
                .build();
    }

    @Test
    void favoriteRoutesExposeListStatusAddAndRemove() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setId(7L);
        PageResponse<ProductResponse> page = new PageResponse<>();
        page.setTotal(1L);
        page.setList(List.of(product));
        when(favoriteService.list(any())).thenReturn(page);
        when(favoriteService.isFavorite(7L)).thenReturn(true);
        when(favoriteService.add(7L)).thenReturn(true);
        when(favoriteService.remove(7L)).thenReturn(true);

        mockMvc.perform(get("/favorite/list").param("page", "1").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value("7"));
        mockMvc.perform(get("/favorite/7/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(post("/favorite/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(delete("/favorite/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void historyRoutesExposeListRecordDeleteAndClear() throws Exception {
        BrowseHistoryResponse item = new BrowseHistoryResponse();
        item.setId(7L);
        item.setViewCount(3);
        PageResponse<BrowseHistoryResponse> page = new PageResponse<>();
        page.setTotal(1L);
        page.setList(List.of(item));
        when(historyService.list(any())).thenReturn(page);
        when(historyService.record(7L)).thenReturn(true);
        when(historyService.remove(7L)).thenReturn(true);
        when(historyService.clear()).thenReturn(true);

        mockMvc.perform(get("/browse-history/list").param("page", "1").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].viewCount").value(3));
        mockMvc.perform(post("/browse-history/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(delete("/browse-history/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(delete("/browse-history/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
