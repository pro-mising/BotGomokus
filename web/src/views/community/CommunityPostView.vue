<template>
    <ContentField>
        <div class="post-detail" v-if="post">
            <article class="post-card">
                <div class="post-head">
                    <img :src="post.author.photo" alt="" class="avatar">
                    <div>
                        <div class="author-line">
                            <span class="author">{{ post.author.username }}</span>
                            <span :class="['presence-badge', post.author.online ? 'is-online' : 'is-offline']">
                                <i></i>{{ post.author.online ? "在线" : "离线" }}
                            </span>
                        </div>
                        <div class="time">{{ post.createtime }}</div>
                    </div>
                    <span class="tag">{{ post.tag }}</span>
                </div>
                <h2>{{ post.title }}</h2>
                <p>{{ post.content }}</p>
                <div class="post-actions">
                    <div class="left-actions">
                        <button :class="post.liked ? 'btn btn-primary btn-sm' : 'btn btn-outline-primary btn-sm'" @click="toggle_like">
                            点赞 {{ post.likes }}
                        </button>
                    </div>
                    <div class="right-actions">
                        <button class="btn btn-outline-secondary btn-sm compact-btn" @click="go_back">返回社区</button>
                        <button v-if="post.can_delete" class="btn btn-outline-danger btn-sm compact-btn" @click="remove_post">
                            删除帖子
                        </button>
                    </div>
                </div>
            </article>

            <section class="comment-box">
                <textarea v-model="commentadd.content" class="form-control" rows="3" placeholder="写下你的评论..."></textarea>
                <div class="comment-actions">
                    <span class="error-message">{{ commentadd.error_message }}</span>
                    <button class="btn btn-primary" @click="add_comment">发表评论</button>
                </div>
            </section>

            <section class="comments">
                <article class="comment-card" v-for="comment in comments" :key="comment.id">
                    <img :src="comment.author.photo" alt="" class="comment-avatar">
                    <div class="comment-body">
                        <div class="comment-head">
                            <strong>{{ comment.author.username }}</strong>
                            <span :class="['presence-badge', comment.author.online ? 'is-online' : 'is-offline']">
                                <i></i>{{ comment.author.online ? "在线" : "离线" }}
                            </span>
                            <span>{{ comment.createtime }}</span>
                            <button v-if="comment.can_delete" class="btn btn-link btn-sm text-danger" @click="remove_comment(comment.id)">
                                删除
                            </button>
                        </div>
                        <p>{{ comment.content }}</p>
                    </div>
                </article>
            </section>
        </div>
    </ContentField>
</template>

<script>
import ContentField from "@/components/ContentField.vue";
import { reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { useStore } from "vuex";
import router from "@/router";
import $ from "jquery";

export default {
    components: {
        ContentField,
    },
    setup() {
        const route = useRoute();
        const store = useStore();
        const postId = parseInt(route.params.postId);
        const post = ref(null);
        const comments = ref([]);

        const commentadd = reactive({
            content: "",
            error_message: "",
        });

        const authHeaders = () => ({
            Authorization: "Bearer " + store.state.user.token,
        });

        const refresh_post = () => {
            $.ajax({
                url: "http://127.0.0.1:3000/community/post/detail/",
                type: "GET",
                data: { post_id: postId },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        post.value = resp.post;
                    }
                }
            });
        };

        const refresh_comments = () => {
            $.ajax({
                url: "http://127.0.0.1:3000/community/comment/list/",
                type: "GET",
                data: { post_id: postId },
                headers: authHeaders(),
                success(resp) {
                    comments.value = resp.comments;
                }
            });
        };

        const toggle_like = () => {
            if (!post.value) return;
            $.ajax({
                url: post.value.liked
                    ? "http://127.0.0.1:3000/community/post/unlike/"
                    : "http://127.0.0.1:3000/community/post/like/",
                type: "POST",
                data: { post_id: postId },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") refresh_post();
                }
            });
        };

        const add_comment = () => {
            commentadd.error_message = "";
            $.ajax({
                url: "http://127.0.0.1:3000/community/comment/add/",
                type: "POST",
                data: {
                    post_id: postId,
                    content: commentadd.content,
                },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        commentadd.content = "";
                        refresh_comments();
                        refresh_post();
                    } else {
                        commentadd.error_message = resp.error_message;
                    }
                }
            });
        };

        const remove_comment = commentId => {
            $.ajax({
                url: "http://127.0.0.1:3000/community/comment/remove/",
                type: "POST",
                data: { comment_id: commentId },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        refresh_comments();
                        refresh_post();
                    }
                }
            });
        };

        const remove_post = () => {
            if (!confirm("确定要删除这篇帖子吗？")) return;
            $.ajax({
                url: "http://127.0.0.1:3000/community/post/remove/",
                type: "POST",
                data: { post_id: postId },
                headers: authHeaders(),
                success(resp) {
                    if (resp.error_message === "success") {
                        router.push({ name: "community_index" });
                    }
                }
            });
        };

        const go_back = () => {
            router.push({ name: "community_index" });
        };

        refresh_post();
        refresh_comments();

        return {
            post,
            comments,
            commentadd,
            toggle_like,
            add_comment,
            remove_comment,
            remove_post,
            go_back,
        };
    }
}
</script>

<style scoped>
.post-detail {
    display: grid;
    gap: 16px;
}

.post-card,
.comment-box,
.comment-card {
    padding: 16px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: white;
}

.post-head {
    display: flex;
    align-items: center;
    gap: 10px;
}

.avatar,
.comment-avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    object-fit: cover;
}

.author {
    font-weight: 800;
}

.author-line {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.presence-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
}

.presence-badge i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
}

.presence-badge.is-online {
    color: #047857;
    background: #d1fae5;
}

.presence-badge.is-online i {
    background: #10b981;
}

.presence-badge.is-offline {
    color: #64748b;
    background: #f1f5f9;
}

.presence-badge.is-offline i {
    background: #94a3b8;
}

.time,
.comment-head span {
    color: #64748b;
    font-size: 13px;
}

.tag {
    margin-left: auto;
    padding: 4px 10px;
    border-radius: 999px;
    background: #e0f2fe;
    color: #075985;
    font-weight: 700;
    font-size: 12px;
}

h2 {
    margin-top: 14px;
}

p {
    white-space: pre-wrap;
}

.comment-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
}

.post-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding-top: 14px;
    border-top: 1px solid #eef2f7;
}

.left-actions,
.right-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
}

.compact-btn {
    min-width: auto;
}

.error-message {
    color: #dc3545;
}

.comments {
    display: grid;
    gap: 10px;
}

.comment-card {
    display: flex;
    gap: 12px;
}

.comment-body {
    flex: 1;
}

.comment-head {
    display: flex;
    align-items: center;
    gap: 10px;
}

@media (max-width: 640px) {
    .post-actions {
        align-items: stretch;
        flex-direction: column;
    }

    .left-actions,
    .right-actions {
        justify-content: flex-start;
    }
}
</style>
