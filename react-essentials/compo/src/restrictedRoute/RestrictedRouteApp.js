import {AuthService} from "./AuthService";
import {BrowserRouter, Link, Navigate, Route, Routes} from "react-router-dom";
import {Login} from "./Login";
import {PrivateRoute} from "./PrivateRoute";
import {Dashboard} from "./Dashboard";
import {useState} from "react";

export const RestrictedRouteApp = () => {
    const [authStatus, setAuthStatus] = useState(AuthService.isAuthenticated);
    const handleLogin = () => {
        AuthService.login(() => {
            setAuthStatus(AuthService.isAuthenticated);
        });
    }
    const handleLogout = () => {
        AuthService.logout(() => {
            setAuthStatus(AuthService.isAuthenticated);
        });
    }
    return (
        <BrowserRouter>
            {/* 상단에 고정된 네비게이션 바 */}
            <nav>
                {AuthService.isAuthenticated ? (
                    <div>
                        <Link to={"/dashboard"}>Dashboard</Link>
                        <button onClick={handleLogout}>Logout</button>
                    </div>
                ) : (
                    <span>Please Login</span>
                )}
            </nav>

            {/* 하단에 동적으로 변하는 컴포넌트 컨텐트 */}
            <Routes>
                {/* 버튼 클릭하면 handleLogin() 실행시킴 */}
                <Route path={"/login"} element={<Login onLogin={handleLogin}/>}/>
                {/* 로그인 상태에 따라 대시보드 컴포넌트를 보여주거나 "/login"으로 이동시킴 */}
                <Route path={"/dashboard"} element={
                    <PrivateRoute>
                        <Dashboard/>
                    </PrivateRoute>
                }/>
                {/* "/"로 접근하면 바로 "/dashboard"로 이동함 */}
                <Route path={"/"} element={<Navigate to={"/dashboard"}/>}/>
            </Routes>
        </BrowserRouter>
    );
}
