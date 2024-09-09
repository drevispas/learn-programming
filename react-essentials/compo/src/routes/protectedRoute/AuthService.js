export const AuthService = {
    isAuthenticated: false,
    login(cb) {
        this.isAuthenticated = true;
        console.log("로그인 상태: ", this.isAuthenticated);
        setTimeout(cb, 100)
    },
    logout(cb) {
        this.isAuthenticated = false;
        console.log("로그인 상태: ", this.isAuthenticated);
        setTimeout(cb, 100)
    }
}
