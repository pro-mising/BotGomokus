<template>
    <ContentField>
        <div class="rank-page">
            <header class="rank-header">
                <div>
                    <h2>排行榜</h2>
                    <p v-if="updated_at" class="updated-time">更新时间：{{ updated_at }}</p>
                </div>
                <div class="summary-chip" v-if="summary">
                    <span>{{ summary.label }}</span>
                    <strong>{{ summary.total }}</strong>
                </div>
            </header>

            <section class="rank-tabs">
                <button
                    v-for="tab in tabs"
                    :key="tab.key"
                    :class="{ active: active_type === tab.key }"
                    @click="change_type(tab.key)"
                >
                    <span>{{ tab.title }}</span>
                    <small>{{ tab.desc }}</small>
                </button>
            </section>

            <section class="leader-panel" v-if="summary && entries.length">
                <div>
                    <span>当前榜首</span>
                    <strong>{{ summary.leader }}</strong>
                </div>
                <div>
                    <span>{{ mainLabel }}</span>
                    <strong>{{ summary.leader_value }}</strong>
                </div>
                <div>
                    <span>榜单说明</span>
                    <strong>{{ activeTab.desc }}</strong>
                </div>
            </section>

            <section class="rank-list" v-if="entries.length">
                <article
                    v-for="entry in entries"
                    :key="`${active_type}-${entry.rank}-${entry.user_id}-${entry.bot_id || 0}`"
                    :class="['rank-card', topClass(entry.rank)]"
                >
                    <div class="rank-no">{{ entry.rank }}</div>
                    <img :src="entry.photo" alt="" class="avatar">
                    <div class="rank-main">
                        <div class="name-line">
                            <strong>{{ displayName(entry) }}</strong>
                            <span>{{ entry.username }}</span>
                        </div>
                        <div class="detail-line">
                            <template v-if="active_type === 'ladder'">
                                对局 {{ entry.games }} · 胜利 {{ entry.wins }} · 胜率 {{ entry.win_rate }}%
                            </template>
                            <template v-else-if="active_type === 'bot'">
                                {{ entry.mode }} · 胜率 {{ entry.win_rate }}% · 平均 {{ entry.average_steps }} 手
                            </template>
                            <template v-else-if="active_type === 'active'">
                                近7天对局 {{ entry.recent_games }} · 发帖 {{ entry.recent_posts }} · 评论 {{ entry.recent_comments }} · 点赞 {{ entry.recent_likes }}
                            </template>
                            <template v-else>
                                发帖 {{ entry.posts }} · 获赞 {{ entry.likes_received }} · 评论 {{ entry.comments_made }}
                            </template>
                        </div>
                        <div class="metric-row" v-if="active_type === 'bot'">
                            <span>进攻 {{ entry.attack_score }}</span>
                            <span>防守 {{ entry.defense_score }}</span>
                            <span>稳定 {{ entry.stability_score }}</span>
                            <span>效率 {{ entry.efficiency_score }}</span>
                        </div>
                    </div>
                    <div class="score-box">
                        <span>{{ entry.main_label }}</span>
                        <strong>{{ entry.main_value }}</strong>
                        <small>{{ entry.sub_value }}</small>
                    </div>
                </article>
            </section>

            <section class="empty-state" v-else>
                当前榜单还没有数据。
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
import { computed, ref } from "vue";
import { useStore } from "vuex";
import $ from "jquery";

export default {
    components: {
        ContentField,
    },
    setup() {
        const store = useStore();
        const tabs = [
            { key: "ladder", title: "天梯榜", desc: "按玩家天梯分排序" },
            { key: "active", title: "活跃榜", desc: "按近7天对局、发帖、评论和点赞排序" },
            { key: "bot", title: "Bot强度榜", desc: "按Bot测评综合评分排序" },
            { key: "community", title: "社区贡献榜", desc: "按发帖、获赞和评论贡献排序" },
        ];
        const active_type = ref("ladder");
        const entries = ref([]);
        const pages = ref([]);
        const summary = ref(null);
        const updated_at = ref("");
        let current_page = 1;
        let total_count = 0;
        const page_size = 10;

        const activeTab = computed(() => tabs.find(tab => tab.key === active_type.value) || tabs[0]);
        const mainLabel = computed(() => {
            if (active_type.value === "bot") return "综合评分";
            if (active_type.value === "community") return "贡献分";
            if (active_type.value === "active") return "活跃分";
            return "天梯分";
        });

        const authHeaders = () => ({
            Authorization: "Bearer " + store.state.user.token,
        });

        const update_pages = () => {
            const max_pages = parseInt(Math.ceil(total_count / page_size));
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
                url: "http://127.0.0.1:3000/ranklist/multilist/",
                type: "GET",
                data: {
                    type: active_type.value,
                    page,
                },
                headers: authHeaders(),
                success(resp) {
                    entries.value = resp.entries || [];
                    total_count = resp.total_count || 0;
                    summary.value = resp.summary || null;
                    updated_at.value = resp.updated_at || "";
                    update_pages();
                },
                error() {
                    entries.value = [];
                    total_count = 0;
                    summary.value = null;
                    updated_at.value = "";
                    update_pages();
                }
            });
        };

        const change_type = type => {
            if (active_type.value === type) return;
            active_type.value = type;
            pull_page(1);
        };

        const click_page = page => {
            if (page === -2) page = current_page - 1;
            else if (page === -1) page = current_page + 1;
            const max_pages = parseInt(Math.ceil(total_count / page_size));
            if (page >= 1 && page <= max_pages) pull_page(page);
        };

        const topClass = rank => {
            if (rank === 1) return "top-one";
            if (rank === 2) return "top-two";
            if (rank === 3) return "top-three";
            return "";
        };

        const displayName = entry => {
            if (active_type.value === "bot") return entry.bot_name || "未命名Bot";
            return entry.username;
        };

        pull_page(current_page);

        return {
            tabs,
            active_type,
            activeTab,
            entries,
            pages,
            summary,
            updated_at,
            mainLabel,
            change_type,
            click_page,
            topClass,
            displayName,
        };
    }
}
</script>

<style scoped>
.rank-page {
    display: grid;
    gap: 18px;
}

.rank-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding-bottom: 10px;
    border-bottom: 1px solid #e5e7eb;
}

.header-kicker {
    color: #c8872c;
    font-size: 13px;
    font-weight: 900;
}

h2 {
    margin: 0;
    color: #0f172a;
    font-size: 28px;
    font-weight: 900;
}

.updated-time {
    margin: 6px 0 0;
    color: #64748b;
    font-size: 13px;
    font-weight: 800;
}

.summary-chip {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    padding: 8px 12px;
    border: 1px solid #eadcc8;
    border-radius: 8px;
    background: #fffaf0;
    color: #92400e;
    font-weight: 900;
}

.summary-chip strong {
    color: #0f172a;
}

.rank-tabs {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
}

.rank-tabs button {
    display: grid;
    gap: 4px;
    min-height: 68px;
    padding: 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
    color: #64748b;
    text-align: left;
    transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.rank-tabs button span {
    color: #0f172a;
    font-size: 16px;
    font-weight: 900;
}

.rank-tabs button small {
    font-weight: 700;
}

.rank-tabs button.active {
    border-color: #d9962b;
    background: linear-gradient(135deg, #fff7e8, #ffffff);
    box-shadow: 0 12px 28px rgba(146, 64, 14, 0.1);
}

.leader-panel {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    padding: 16px;
    border: 1px solid #eadcc8;
    border-radius: 8px;
    background: linear-gradient(135deg, #fffaf0, #ffffff);
}

.leader-panel div {
    display: grid;
    gap: 4px;
}

.leader-panel span {
    color: #64748b;
    font-size: 13px;
    font-weight: 800;
}

.leader-panel strong {
    color: #0f172a;
    font-size: 20px;
    font-weight: 900;
}

.rank-list {
    display: grid;
    gap: 12px;
}

.rank-card {
    display: grid;
    grid-template-columns: 46px 54px minmax(0, 1fr) minmax(120px, auto);
    gap: 14px;
    align-items: center;
    padding: 16px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.rank-card.top-one,
.rank-card.top-two,
.rank-card.top-three {
    border-color: #eadcc8;
    background: linear-gradient(135deg, #fffaf0, #ffffff);
}

.rank-no {
    width: 38px;
    height: 38px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: #f1f5f9;
    color: #475569;
    font-weight: 900;
}

.top-one .rank-no {
    background: #f6c453;
    color: #3f2a05;
}

.top-two .rank-no {
    background: #dbe4ee;
    color: #334155;
}

.top-three .rank-no {
    background: #f2b57c;
    color: #4a2506;
}

.avatar {
    width: 54px;
    height: 54px;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid #f8ead4;
}

.rank-main {
    min-width: 0;
    display: grid;
    gap: 6px;
}

.name-line {
    display: flex;
    align-items: baseline;
    gap: 10px;
    flex-wrap: wrap;
}

.name-line strong {
    color: #0f172a;
    font-size: 18px;
    font-weight: 900;
}

.name-line span {
    color: #64748b;
    font-weight: 800;
}

.detail-line,
.metric-row {
    color: #64748b;
    font-size: 13px;
    font-weight: 700;
}

.metric-row {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.metric-row span {
    padding: 3px 8px;
    border-radius: 999px;
    background: #f8fafc;
    border: 1px solid #e5e7eb;
}

.score-box {
    min-width: 118px;
    display: grid;
    justify-items: end;
    gap: 2px;
}

.score-box span,
.score-box small {
    color: #64748b;
    font-size: 12px;
    font-weight: 800;
}

.score-box strong {
    color: #92400e;
    font-size: 26px;
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

@media (max-width: 900px) {
    .rank-tabs {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 760px) {
    .rank-header,
    .leader-panel,
    .rank-card {
        grid-template-columns: 1fr;
    }

    .rank-tabs {
        grid-template-columns: 1fr;
    }

    .rank-card {
        justify-items: start;
    }

    .score-box {
        justify-items: start;
    }
}
</style>
