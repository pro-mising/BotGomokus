import $ from "jquery";

const noop = () => {};
const callback = fn => (typeof fn === "function" ? fn : noop);
const isJwt = token => typeof token === "string" && token.split(".").length === 3;

const ajaxError = (message, resp) => ({
    error_message: message,
    status: resp && resp.status,
    responseText: resp && resp.responseText,
});

export default {
    state: {
        id: "",
        username: "",
        photo: "",
        token: "",
        is_login: false,
        online: false,
        pulling_info: true,
    },
    getters: {
    },
    mutations: {
        updateUser(state, user) {
            state.id = user.id;
            state.username = user.username;
            state.photo = user.photo;
            state.is_login = user.is_login;
            state.online = user.online === true || user.online === "true";
        },
        updateOnline(state, online) {
            state.online = online === true || online === "true";
        },
        updateToken(state, token) {
            state.token = token;
        },
        logout(state) {
            state.id = "";
            state.username = "";
            state.photo = "";
            state.token = "";
            state.is_login = false;
            state.online = false;
        },
        updatePullingInfo(state, pulling_info) {
            state.pulling_info = pulling_info;
        }
    },
    actions: {
        login(context, data) {
            const onSuccess = callback(data && data.success);
            const onError = callback(data && data.error);

            sessionStorage.removeItem("jwt_token");
            context.commit("logout");

            $.ajax({
                url: "http://127.0.0.1:3000/user/account/token/",
                type: "post",
                data: {
                    username: data.username,
                    password: data.password,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        sessionStorage.setItem("jwt_token", resp.token);
                        context.commit("updateToken", resp.token);
                        onSuccess(resp);
                    } else {
                        onError(resp);
                    }
                },
                error(resp) {
                    console.error("login request failed:", resp);
                    onError(ajaxError("request failed", resp));
                }
            });
        },
        getinfo(context, data = {}) {
            const onSuccess = callback(data.success);
            const onError = callback(data.error);

            if (!context.state.token) {
                context.commit("logout");
                sessionStorage.removeItem("jwt_token");
                onError({ error_message: "missing token" });
                return;
            }

            if (!isJwt(context.state.token)) {
                context.commit("logout");
                sessionStorage.removeItem("jwt_token");
                onError({ error_message: "invalid token" });
                return;
            }

            $.ajax({
                url: "http://127.0.0.1:3000/user/account/info/",
                type: "get",
                headers: {
                    Authorization: "Bearer " + context.state.token,
                },
                success(resp) {
                    if (resp.error_message === "success") {
                        context.commit("updateUser", {
                            ...resp,
                            is_login: true,
                        });
                        onSuccess(resp);
                    } else {
                        onError(resp);
                    }
                },
                error(resp) {
                    console.error("get user info failed:", resp);
                    context.commit("logout");
                    sessionStorage.removeItem("jwt_token");
                    onError(ajaxError("request failed", resp));
                }
            });
        },
        logout(context) {
            sessionStorage.removeItem("jwt_token");
            context.commit("logout");
        }
    },
    modules: {
    }
};
