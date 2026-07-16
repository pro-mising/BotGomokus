<template>
    <ContentField>
        <div class="profile-page">
            <section class="profile-hero">
                <div class="profile-main">
                    <img :src="profile.photo || defaultAvatar" alt="" class="profile-avatar">
                    <div>
                        <div class="profile-status">
                            <span :class="['status-dot', profile.online ? 'online' : 'offline']"></span>
                            {{ profile.online ? "在线" : "离线" }}
                        </div>
                        <h2>{{ profile.username || "用户" }}</h2>
                        <p>用户ID：{{ profile.id || "-" }}</p>
                    </div>
                </div>
                <div class="profile-rank">
                    <span>天梯分</span>
                    <strong>{{ profile.rating || 0 }}</strong>
                    <small>当前名次：第 {{ profile.ladder_rank || "-" }} 名</small>
                </div>
            </section>

            <section class="stat-grid">
                <article class="stat-card">
                    <span>总对局</span>
                    <strong>{{ battle.total_games || 0 }}</strong>
                    <small>胜率 {{ battle.win_rate || 0 }}%</small>
                </article>
                <article class="stat-card">
                    <span>胜 / 负 / 平</span>
                    <strong>{{ battle.wins || 0 }} / {{ battle.losses || 0 }} / {{ battle.draws || 0 }}</strong>
                    <small>来自历史对局统计</small>
                </article>
                <article class="stat-card">
                    <span>Bot数量</span>
                    <strong>{{ bot.bot_count || 0 }}</strong>
                    <small>最高评分 {{ bot.best_score || 0 }}</small>
                </article>
                <article class="stat-card">
                    <span>社区贡献</span>
                    <strong>{{ community.contribution_score || 0 }}</strong>
                    <small>发帖 {{ community.posts || 0 }} · 获赞 {{ community.likes_received || 0 }}</small>
                </article>
            </section>

            <section class="profile-layout">
                <article class="panel battle-panel">
                    <div class="panel-head">
                        <div>
                            <h3>对战数据</h3>
                            <p>展示总对局、胜负和最近对局入口</p>
                        </div>
                        <router-link :to="{name: 'record_index'}" class="panel-link">进入对局列表</router-link>
                    </div>
                    <div class="battle-content">
                        <div class="battle-summary">
                            <div>
                                <span>总对局</span>
                                <strong>{{ battle.total_games || 0 }}</strong>
                            </div>
                            <div>
                                <span>胜率</span>
                                <strong>{{ battle.win_rate || 0 }}%</strong>
                            </div>
                            <div>
                                <span>战绩</span>
                                <strong>{{ battle.wins || 0 }}胜 {{ battle.losses || 0 }}负 {{ battle.draws || 0 }}平</strong>
                            </div>
                        </div>
                        <div class="recent-list" v-if="battle.recent_records && battle.recent_records.length">
                            <div class="recent-item" v-for="record in battle.recent_records" :key="record.id">
                                <div>
                                    <strong>对阵 {{ record.opponent }}</strong>
                                    <span>{{ record.createtime }}</span>
                                </div>
                                <router-link :to="{name: 'record_content', params: {recordId: record.id}}" :class="['result-pill', resultClass(record.result)]">
                                    {{ record.result }}
                                </router-link>
                            </div>
                        </div>
                        <div class="empty-state" v-else>还没有最近对局。</div>
                    </div>
                </article>

                <article class="panel">
                    <div class="panel-head">
                        <div>
                            <h3>Bot 数据</h3>
                            <p>展示我的 Bot 数量、最近测评和最高评分</p>
                        </div>
                        <router-link :to="{name: 'bot_evaluation'}" class="panel-link">进入Bot测试</router-link>
                    </div>
                    <div class="info-list">
                        <div>
                            <span>最近测评 Bot</span>
                            <strong>{{ bot.latest_bot_name || "暂无测评" }}</strong>
                        </div>
                        <div>
                            <span>最近测评模式</span>
                            <strong>{{ bot.latest_mode || "暂无" }}</strong>
                        </div>
                        <div>
                            <span>最高评分 Bot</span>
                            <strong>{{ bot.best_bot_name || "暂无" }}</strong>
                        </div>
                    </div>
                </article>

                <article class="panel">
                    <div class="panel-head">
                        <div>
                            <h3>社区数据</h3>
                            <p>展示发帖、获赞、评论和社区贡献分</p>
                        </div>
                        <router-link :to="{name: 'community_index'}" class="panel-link">进入社区</router-link>
                    </div>
                    <div class="community-grid">
                        <div>
                            <span>发帖数</span>
                            <strong>{{ community.posts || 0 }}</strong>
                        </div>
                        <div>
                            <span>获赞数</span>
                            <strong>{{ community.likes_received || 0 }}</strong>
                        </div>
                        <div>
                            <span>发出评论</span>
                            <strong>{{ community.comments_made || 0 }}</strong>
                        </div>
                        <div>
                            <span>收到评论</span>
                            <strong>{{ community.comments_received || 0 }}</strong>
                        </div>
                    </div>
                </article>
            </section>
        </div>
    </ContentField>
</template>

<script>
import ContentField from "@/components/ContentField.vue";
import { ref } from "vue";
import { useStore } from "vuex";
import $ from "jquery";

export default {
    components: {
        ContentField,
    },
    setup() {
        const store = useStore();
        const defaultAvatar = "https://cdn.acwing.com/media/user/profile/photo/1_lg_92c1a99441.jpg";
        const profile = ref({});
        const battle = ref({});
        const bot = ref({});
        const community = ref({});

        const pullOverview = () => {
            $.ajax({
                url: "http://127.0.0.1:3000/user/account/profile/overview/",
                type: "GET",
                headers: {
                    Authorization: "Bearer " + store.state.user.token,
                },
                success(resp) {
                    if (resp.error_message !== "success") return;
                    profile.value = resp.profile || {};
                    battle.value = resp.battle || {};
                    bot.value = resp.bot || {};
                    community.value = resp.community || {};
                }
            });
        };

        const resultClass = result => {
            if (result === "胜利") return "win";
            if (result === "失败") return "lose";
            return "draw";
        };

        pullOverview();

        return {
            defaultAvatar,
            profile,
            battle,
            bot,
            community,
            resultClass,
        };
    }
}
</script>

<style scoped>
.profile-page {
    display: grid;
    gap: 18px;
}

.profile-hero {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 22px;
    border: 1px solid #eadcc8;
    border-radius: 8px;
    background: linear-gradient(135deg, #fffaf0, #ffffff);
}

.profile-main {
    display: flex;
    align-items: center;
    gap: 16px;
}

.profile-avatar {
    width: 88px;
    height: 88px;
    border-radius: 50%;
    object-fit: cover;
    border: 4px solid #f8ead4;
    box-shadow: 0 12px 24px rgba(146, 64, 14, 0.12);
}

.profile-status {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #64748b;
    font-size: 13px;
    font-weight: 800;
}

.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #94a3b8;
}

.status-dot.online {
    background: #35b879;
    box-shadow: 0 0 0 3px rgba(53, 184, 121, 0.16);
}

h2,
h3,
p {
    margin: 0;
}

h2 {
    margin-top: 4px;
    color: #0f172a;
    font-size: 30px;
    font-weight: 900;
}

.profile-main p,
.panel-head p {
    color: #64748b;
    font-weight: 700;
}

.profile-rank {
    display: grid;
    justify-items: end;
    gap: 2px;
}

.profile-rank span,
.profile-rank small,
.stat-card span,
.stat-card small,
.battle-summary span,
.info-list span,
.community-grid span {
    color: #64748b;
    font-size: 13px;
    font-weight: 800;
}

.profile-rank strong {
    color: #92400e;
    font-size: 36px;
    font-weight: 900;
}

.stat-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
}

.stat-card,
.panel {
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 8px 22px rgba(15, 23, 42, 0.05);
}

.stat-card {
    display: grid;
    gap: 4px;
    padding: 16px;
}

.stat-card strong {
    color: #0f172a;
    font-size: 24px;
    font-weight: 900;
}

.battle-summary strong {
    color: #0f172a;
    font-size: 20px;
    font-weight: 900;
}

.profile-layout {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

.battle-panel {
    grid-column: 1 / -1;
}

.battle-content {
    display: grid;
    grid-template-columns: 280px minmax(0, 1fr);
    gap: 14px;
    padding-top: 14px;
}

.battle-summary {
    display: grid;
    gap: 10px;
}

.battle-summary div {
    display: grid;
    gap: 4px;
    padding: 12px;
    border-radius: 8px;
    background: #fffaf0;
}

.panel {
    padding: 18px;
}

.panel-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    padding-bottom: 14px;
    border-bottom: 1px solid #eef2f7;
}

h3 {
    color: #0f172a;
    font-size: 18px;
    font-weight: 900;
}

.panel-link {
    white-space: nowrap;
    padding: 7px 10px;
    border-radius: 8px;
    background: #fff7e8;
    color: #92400e;
    font-size: 13px;
    font-weight: 900;
    text-decoration: none;
}

.recent-list,
.info-list {
    display: grid;
    gap: 10px;
}

.panel > .info-list {
    padding-top: 14px;
}

.recent-item,
.info-list div {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 10px;
    border-radius: 8px;
    background: #f8fafc;
}

.recent-item div,
.info-list div {
    min-width: 0;
}

.recent-item strong,
.info-list strong,
.community-grid strong {
    color: #0f172a;
    font-weight: 900;
}

.recent-item span {
    display: block;
    color: #64748b;
    font-size: 12px;
    font-weight: 700;
}

.result-pill {
    min-width: 46px;
    padding: 5px 8px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 900;
    text-align: center;
    text-decoration: none;
}

.result-pill.win {
    background: #ecfdf5;
    color: #047857;
}

.result-pill.lose {
    background: #fff1f2;
    color: #be123c;
}

.result-pill.draw {
    background: #f1f5f9;
    color: #475569;
}

.community-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    padding-top: 14px;
}

.community-grid div {
    display: grid;
    gap: 4px;
    padding: 12px;
    border-radius: 8px;
    background: #f8fafc;
}

.empty-state {
    margin-top: 14px;
    padding: 28px 12px;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    color: #64748b;
    text-align: center;
    font-weight: 800;
}

@media (max-width: 900px) {
    .stat-grid,
    .profile-layout,
    .battle-content {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 680px) {
    .profile-hero,
    .profile-main,
    .panel-head {
        align-items: flex-start;
        flex-direction: column;
    }

    .profile-rank {
        justify-items: start;
    }

    .stat-grid,
    .profile-layout,
    .battle-content,
    .community-grid {
        grid-template-columns: 1fr;
    }
}
</style>
