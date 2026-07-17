<template>
    <div class="record-content-page">
        <button class="back-record-list-btn" @click="go_back">返回对局列表</button>
        <PlayGround />
    </div>
</template>

<script>
import PlayGround from "@/components/PlayGround.vue";
import router from "@/router";
import { useStore } from "vuex";

export default {
    components: {
        PlayGround,
    },
    setup() {
        const store = useStore();
        const go_back = () => {
            store.commit("updateIsRecord", false);
            router.push({ name: "record_index" });
        };

        return {
            go_back,
        };
    }
}


</script>

<style scoped>
.record-content-page {
    position: relative;
}

.back-record-list-btn {
    position: fixed;
    z-index: 20;
    left: 28px;
    top: 86px;
    min-height: 38px;
    padding: 7px 16px;
    border: 1px solid rgba(217, 150, 43, 0.35);
    border-radius: 8px;
    background: rgba(255, 248, 237, 0.92);
    color: #92400e;
    font-size: 14px;
    font-weight: 900;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.back-record-list-btn:hover {
    background: #fff1d8;
}
</style>
