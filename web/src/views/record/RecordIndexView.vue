<template>
    <ContentField>
        <div class="record-center">
            <header class="record-header">
                <div>
                    <h2>对局列表</h2>
                </div>
            </header>

            <section class="toolbar">
                <div class="search-box">
                    <input v-model="filters.keyword" class="form-control" placeholder="搜索玩家、关键手或复盘摘要" @keyup.enter="run_search">
                    <button class="btn btn-primary" @click="run_search">搜索</button>
                </div>
                <select v-model="filters.result" class="form-select" @change="run_search">
                    <option value="all">全部结果</option>
                    <option value="a_win">黑方胜</option>
                    <option value="b_win">白方胜</option>
                    <option value="draw">平局</option>
                </select>
                <select v-model="filters.sort" class="form-select" @change="run_search">
                    <option value="time">最新对局</option>
                    <option value="hot">热门对局</option>
                    <option value="steps">手数参考</option>
                </select>
                <button :class="filters.favorite_only ? 'btn btn-primary' : 'btn btn-outline-primary'" @click="toggle_favorite_filter">
                    我的收藏
                </button>
            </section>

            <section class="record-list" v-if="records.length">
                <article class="record-card" v-for="record in records" :key="record.record.id">
                    <div class="players">
                        <div class="player">
                            <img :src="record.a_photo" alt="" class="avatar">
                            <div>
                                <span class="side-label"><i class="stone-dot black"></i>黑方</span>
                                <strong v-html="highlightText(record.highlight && record.highlight.black_username, record.a_username)"></strong>
                            </div>
                        </div>
                        <div :class="['result-badge', winnerClass(record)]">
                            <i v-if="winnerClass(record) !== 'draw'" :class="['stone-dot', winnerClass(record) === 'black-win' ? 'black' : 'white']"></i>
                            <span>{{ winnerLabel(record) }}</span>
                        </div>
                        <div class="player right">
                            <img :src="record.b_photo" alt="" class="avatar">
                            <div>
                                <span class="side-label"><i class="stone-dot white"></i>白方</span>
                                <strong v-html="highlightText(record.highlight && record.highlight.white_username, record.b_username)"></strong>
                            </div>
                        </div>
                    </div>

                    <div class="analysis">
                        <div class="analysis-metrics">
                            <span :class="['score-pill', scoreLevel(record.analysis.highlight_score)]">
                                {{ record.analysis.highlight_score }} 分
                            </span>
                            <span>总手数 {{ record.analysis.total_steps }}</span>
                            <span>{{ record.analysis.win_direction || "待分析" }}</span>
                            <span>收藏 {{ record.favorite_count }}</span>
                        </div>
                    </div>

                    <div class="actions">
                        <button :class="record.favorite ? 'btn btn-primary record-action-btn' : 'btn btn-outline-primary record-action-btn'" @click="toggle_favorite(record)">
                            {{ record.favorite ? "已收藏" : "收藏" }}
                        </button>
                        <button class="btn btn-outline-secondary record-action-btn" @click="open_record_content(record.record.id)">
                            查看回放
                        </button>
                        <div class="time-row">{{ record.record.createtime }}</div>
                    </div>
                </article>
            </section>

            <section class="empty-state" v-else>
                暂时没有符合条件的对局。
            </section>

            <nav v-if="pages.length">
                <ul class="pagination justify-content-end">
                    <li class="page-item" @click="click_page(-2)">
                        <a href="#" class="page-link">上一页</a>
                    </li>
                    <li :class="'page-item ' + page.is_active" v-for="page in pages" :key="page.number" @click="click_page(page.number)">
                        <a class="page-link" href="#">{{ page.number }}</a>
                    </li>
                    <li class="page-item" @click="click_page(-1)">
                        <a href="#" class="page-link">下一页</a>
                    </li>
                </ul>
            </nav>
        </div>
    </ContentField>
</template>

<script>
import ContentField from "@/components/ContentField.vue";
import { reactive, ref } from "vue";
import { useStore } from "vuex";
import $ from "jquery";
import router from "@/router";

export default {
    components: {
        ContentField,
    },
    setup() {
        const store = useStore();
        const records = ref([]);
        const pages = ref([]);
        const filters = reactive({
            keyword: "",
            result: "all",
            sort: "time",
            favorite_only: false,
        });
        let current_page = 1;
        let total_records = 0;
        const page_size = 10;

        const authHeaders = () => ({
            Authorization: "Bearer " + store.state.user.token,
        });

        const update_pages = () => {
            const max_pages = parseInt(Math.ceil(total_records / page_size));
            const new_pages = [];
            for (let i = current_page - 2; i <= current_page + 2; i++) {
                if (i >= 1 && i <= max_pages) {
                    new_pages.push({
                        number: i,
                        is_active: i === current_page ? "active" : "",
                    });
                }
            }
            pages.value = new_pages;
        };

        const pull_page = page => {
            current_page = page;
            $.ajax({
                url: "http://127.0.0.1:3000/record/center/list/",
                type: "GET",
                data: {
                    page,
                    keyword: filters.keyword.trim(),
                    result: filters.result,
                    sort: filters.sort,
                    favorite_only: filters.favorite_only,
                },
                headers: authHeaders(),
                success(resp) {
                    records.value = resp.records || [];
                    total_records = resp.record_count || 0;
                    update_pages();
                }
            });
        };

        const run_search = () => pull_page(1);

        const toggle_favorite_filter = () => {
            filters.favorite_only = !filters.favorite_only;
            pull_page(1);
        };

        const click_page = page => {
            if (page === -2) page = current_page - 1;
            else if (page === -1) page = current_page + 1;
            const max_pages = parseInt(Math.ceil(total_records / page_size));
            if (page >= 1 && page <= max_pages) pull_page(page);
        };

        const toggle_favorite = record => {
            $.ajax({
                url: record.favorite
                    ? "http://127.0.0.1:3000/record/favorite/remove/"
                    : "http://127.0.0.1:3000/record/favorite/add/",
                type: "GET",
                data: { record_id: record.record.id },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        record.favorite = !record.favorite;
                        record.favorite_count += record.favorite ? 1 : -1;
                        if (record.favorite_count < 0) record.favorite_count = 0;
                    }
                }
            });
        };

        const cleanSummary = (summary, keyMoment) => {
            let text = String(summary || "").trim();
            const key = String(keyMoment || "").trim();
            if (!text) return "分析结果生成中，稍后刷新即可查看完整复盘摘要。";
            if (key && text.endsWith(key)) {
                text = text.slice(0, -key.length).trim();
            }
            return text || key || "分析结果生成中，稍后刷新即可查看完整复盘摘要。";
        };

        const scoreLevel = score => {
            const value = Number(score || 0);
            if (value >= 80) return "high";
            if (value >= 45) return "mid";
            return "low";
        };

        const escapeHtml = text => String(text || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

        const highlightText = (highlight, fallback) => {
            let raw = highlight && String(highlight).trim() ? highlight : fallback;
            if ((!highlight || !String(highlight).trim()) && filters.keyword.trim()) {
                raw = applyLocalHighlight(raw, filters.keyword.trim());
            }
            return escapeHtml(raw)
                .replace(/&lt;em&gt;/g, "<em>")
                .replace(/&lt;\/em&gt;/g, "</em>");
        };

        const applyLocalHighlight = (text, keyword) => {
            const source = String(text || "");
            const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
            if (!escapedKeyword) return source;
            return source.replace(new RegExp(escapedKeyword, "gi"), match => `<em>${match}</em>`);
        };

        const stringTo2D = map => {
            const g = [];
            for (let i = 0, k = 0; i < 15; i++) {
                const line = [];
                for (let j = 0; j < 15; j++, k++) {
                    if (!map || !map[k] || map[k] === "0") line.push(0);
                    else if (map[k] === "1") line.push(1);
                    else line.push(2);
                }
                g.push(line);
            }
            return g;
        };

        const open_record_content = recordId => {
            for (const record of records.value) {
                if (record.record.id === recordId) {
                    store.commit("updateIsRecord", true);
                    store.commit("updateGame", {
                        map: stringTo2D(record.record.map),
                        a_id: record.record.aid,
                        a_sx: record.record.asx,
                        a_sy: record.record.asy,
                        b_id: record.record.bid,
                        b_sx: record.record.bsx,
                        b_sy: record.record.bsy,
                        current_player: record.record.aid,
                    });
                    store.commit("updateSteps", {
                        a_steps: record.record.asteps,
                        b_steps: record.record.bsteps,
                    });
                    store.commit("updateRecordLoser", record.record.loser);
                    router.push({
                        name: "record_content",
                        params: { recordId },
                    });
                    break;
                }
            }
        };

        const winnerClass = record => {
            const loser = record && record.record ? record.record.loser : "";
            if (loser === "A") return "white-win";
            if (loser === "B") return "black-win";
            return "draw";
        };

        const winnerLabel = record => {
            const type = winnerClass(record);
            if (type === "black-win") return "黑胜";
            if (type === "white-win") return "白胜";
            return "平局";
        };

        pull_page(current_page);

        return {
            records,
            pages,
            filters,
            click_page,
            open_record_content,
            run_search,
            toggle_favorite,
            toggle_favorite_filter,
            cleanSummary,
            scoreLevel,
            highlightText,
            winnerClass,
            winnerLabel,
        };
    }
}
</script>

<style scoped>
.record-center {
    display: grid;
    gap: 16px;
}

.record-header {
    padding-bottom: 12px;
    border-bottom: 1px solid #e5e7eb;
}

h2 {
    margin: 0;
    color: #0f172a;
    font-size: 30px;
    font-weight: 900;
}

.toolbar {
    display: grid;
    grid-template-columns: minmax(260px, 1fr) 140px 140px auto;
    gap: 10px;
    padding: 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #f8fafc;
}

.search-box {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 10px;
}

.record-list {
    display: grid;
    gap: 12px;
}

.record-card {
    display: grid;
    grid-template-columns: minmax(330px, 0.95fr) minmax(330px, 1.15fr) 148px;
    gap: 22px;
    align-items: center;
    padding: 18px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

.players {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    gap: 10px;
    align-items: center;
}

.player {
    display: flex;
    align-items: center;
    gap: 12px;
}

.player > div {
    display: grid;
    align-content: center;
    gap: 4px;
    min-height: 68px;
}

.player.right {
    flex-direction: row-reverse;
    text-align: right;
}

.player.right > div {
    justify-items: end;
}

.avatar {
    width: 68px;
    height: 68px;
    min-width: 68px;
    min-height: 68px;
    aspect-ratio: 1 / 1;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid #f1f5f9;
    box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.player span,
.time-row {
    color: #64748b;
    font-size: 12px;
    font-weight: 800;
}

.side-label {
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.stone-dot {
    display: inline-block;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.2);
}

.stone-dot.black {
    background: radial-gradient(circle at 35% 30%, #5b5b5b, #111827 62%, #020617);
}

.stone-dot.white {
    border: 1px solid #d1d5db;
    background: radial-gradient(circle at 35% 30%, #ffffff, #f3f4f6 62%, #d1d5db);
}

.player strong {
    display: block;
    color: #0f172a;
    font-size: 17px;
    font-weight: 900;
}

.result-badge {
    min-width: 64px;
    min-height: 34px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 6px 10px;
    border-radius: 999px;
    border: 1px solid #eadcc8;
    text-align: center;
    font-size: 13px;
    font-weight: 900;
}

.result-badge.black-win {
    background: #f8fafc;
    color: #111827;
}

.result-badge.white-win {
    background: #fffaf0;
    color: #92400e;
}

.result-badge.draw {
    background: #f1f5f9;
    color: #64748b;
}

.analysis {
    display: grid;
    min-width: 0;
    padding: 2px 0 2px 22px;
    border-left: 1px solid #eef2f7;
}

.record-card :deep(em) {
    padding: 0 3px;
    border-radius: 4px;
    background: #ffe1a6;
    color: #92400e;
    font-style: normal;
    font-weight: 900;
}

.analysis-metrics {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.analysis-metrics span {
    display: inline-flex;
    align-items: center;
    min-height: 34px;
    padding: 6px 11px;
    border: 1px solid #e5e7eb;
    border-radius: 999px;
    background: #f8fafc;
    color: #334155;
    font-size: 13px;
    font-weight: 900;
    text-align: center;
    line-height: 1.25;
}

.score-pill {
    border-color: rgba(217, 150, 43, 0.34) !important;
    color: #92400e !important;
    background: #fff1d8 !important;
}

.score-pill.high {
    border-color: rgba(5, 150, 105, 0.25) !important;
    color: #047857 !important;
    background: #d1fae5 !important;
}

.score-pill.low {
    border-color: rgba(100, 116, 139, 0.22) !important;
    color: #64748b !important;
    background: #f8fafc !important;
}

.time-row {
    justify-self: center;
    margin-top: 2px;
    color: #94a3b8;
    font-size: 11px;
    line-height: 1.35;
    text-align: center;
}

.actions {
    display: grid;
    gap: 11px;
    min-width: 148px;
    justify-items: stretch;
}

.record-action-btn {
    width: 100%;
    min-height: 42px;
    padding: 9px 16px;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 900;
}

.empty-state {
    padding: 42px 18px;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    background: #f8fafc;
    color: #64748b;
    text-align: center;
    font-weight: 800;
}

@media (max-width: 980px) {
    .record-header,
    .toolbar,
    .record-card {
        grid-template-columns: 1fr;
    }

    .analysis {
        padding-left: 0;
        padding-top: 12px;
        border-left: 0;
        border-top: 1px solid #eef2f7;
    }
}
</style>
