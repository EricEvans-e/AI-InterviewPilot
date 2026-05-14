import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/constants";
import { useAppDispatch } from "@/store/hooks";
import { loginUser } from "@/store/slices/userSlice";
import { authService } from "@/services/authService";
import type {
  AuthFormData,
  AuthMode,
} from "@/hooks/auth/useAuthPageController";

type AuthFormCardProps = {
  mode: AuthMode;
  formData: AuthFormData;
  errorMessage: string;
  isSubmitting: boolean;
  onSwitchMode: (mode: AuthMode) => void;
  onInputChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onSubmit: () => void;
};

export default function AuthFormCard({
  mode,
  formData,
  errorMessage,
  isSubmitting,
  onSwitchMode,
  onInputChange,
  onSubmit,
}: AuthFormCardProps) {
  const isLogin = mode === "login";

  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  // Phone login state
  const [loginMode, setLoginMode] = useState<"password" | "phone">("password");
  const [phone, setPhone] = useState("");
  const [smsCode, setSmsCode] = useState("");
  const [phoneError, setPhoneError] = useState("");
  const [phoneSubmitting, setPhoneSubmitting] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [smsSending, setSmsSending] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Countdown timer effect
  useEffect(() => {
    if (countdown > 0) {
      timerRef.current = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            if (timerRef.current) clearInterval(timerRef.current);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [countdown]);

  const handleSendCode = async () => {
    if (!phone || !/^1[3-9]\d{9}$/.test(phone)) {
      setPhoneError("请输入正确的手机号");
      return;
    }
    setPhoneError("");
    setSmsSending(true);
    try {
      await authService.sendSmsCode(phone);
      setCountdown(60);
    } catch {
      setPhoneError("验证码发送失败，请稍后重试");
    } finally {
      setSmsSending(false);
    }
  };

  const handlePhoneLogin = async (e: FormEvent) => {
    e.preventDefault();
    if (!phone || !/^1[3-9]\d{9}$/.test(phone)) {
      setPhoneError("请输入正确的手机号");
      return;
    }
    if (!smsCode) {
      setPhoneError("请输入验证码");
      return;
    }
    setPhoneError("");
    setPhoneSubmitting(true);
    try {
      const user = await authService.phoneLogin(phone, smsCode);
      dispatch(loginUser.fulfilled(user, "", { username: "", password: "" }));
      // Role-based redirect
      const userRole = user.role ?? "student";
      switch (userRole) {
        case "admin":
          navigate(ROUTES.adminDashboard);
          break;
        case "teacher":
          navigate(ROUTES.teacherDashboard);
          break;
        default:
          navigate(ROUTES.lobby);
      }
    } catch {
      setPhoneError("手机号登录失败，请检查验证码");
    } finally {
      setPhoneSubmitting(false);
    }
  };

  return (
    <Card className="p-8 border-slate-100 shadow-sm">
      <div className="flex items-center gap-2 rounded-full bg-slate-100 p-1">
        <Button
          type="button"
          variant={isLogin ? "default" : "ghost"}
          className={cn("flex-1 rounded-full", !isLogin && "text-slate-500")}
          onClick={() => onSwitchMode("login")}
        >
          登录
        </Button>
        <Button
          type="button"
          variant={!isLogin ? "default" : "ghost"}
          className={cn("flex-1 rounded-full", isLogin && "text-slate-500")}
          onClick={() => onSwitchMode("register")}
        >
          注册
        </Button>
      </div>

      {isLogin && (
        <div className="flex gap-2 mt-6 mb-4">
          <button
            type="button"
            className={cn(
              "flex-1 py-2 text-sm rounded-md transition-colors",
              loginMode === "password"
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80",
            )}
            onClick={() => {
              setLoginMode("password");
              setPhoneError("");
            }}
          >
            密码登录
          </button>
          <button
            type="button"
            className={cn(
              "flex-1 py-2 text-sm rounded-md transition-colors",
              loginMode === "phone"
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80",
            )}
            onClick={() => {
              setLoginMode("phone");
              setPhoneError("");
            }}
          >
            验证码登录
          </button>
        </div>
      )}

      {isLogin && loginMode === "phone" ? (
        <form onSubmit={handlePhoneLogin}>
          <div className="mt-2 space-y-4">
            <div className="space-y-2">
              <label className="text-xs text-slate-500">手机号</label>
              <Input
                placeholder="请输入手机号"
                value={phone}
                onChange={(e) => {
                  setPhone(e.target.value);
                  setPhoneError("");
                }}
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs text-slate-500">验证码</label>
              <div className="flex gap-2">
                <Input
                  placeholder="请输入验证码"
                  value={smsCode}
                  onChange={(e) => {
                    setSmsCode(e.target.value);
                    setPhoneError("");
                  }}
                />
                <Button
                  type="button"
                  variant="outline"
                  className="shrink-0 whitespace-nowrap"
                  onClick={handleSendCode}
                  disabled={countdown > 0 || smsSending}
                >
                  {smsSending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : countdown > 0 ? (
                    `${countdown}s`
                  ) : (
                    "发送验证码"
                  )}
                </Button>
              </div>
            </div>

            {(phoneError || errorMessage) && (
              <div className="text-xs text-red-500">
                {phoneError || errorMessage}
              </div>
            )}
          </div>

          <Button
            type="submit"
            className="w-full mt-6 rounded-full"
            disabled={phoneSubmitting}
          >
            {phoneSubmitting && (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            )}
            登录进入
          </Button>
        </form>
      ) : (
        <>
          <div className="mt-6 space-y-4">
            <div className="space-y-2">
              <label className="text-xs text-slate-500">用户名</label>
              <Input
                name="username"
                placeholder="输入你的用户名"
                value={formData.username}
                onChange={onInputChange}
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs text-slate-500">密码</label>
              <Input
                name="password"
                type="password"
                placeholder="输入密码"
                value={formData.password}
                onChange={onInputChange}
              />
            </div>
            {!isLogin && (
              <div className="space-y-2">
                <label className="text-xs text-slate-500">确认密码</label>
                <Input
                  name="confirmPassword"
                  type="password"
                  placeholder="再次输入密码"
                  value={formData.confirmPassword}
                  onChange={onInputChange}
                />
              </div>
            )}

            {errorMessage && (
              <div className="text-xs text-red-500 mt-2">{errorMessage}</div>
            )}
          </div>

          <Button
            className="w-full mt-6 rounded-full"
            onClick={onSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting && (
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            )}
            {isLogin ? "登录进入" : "注册并开始"}
          </Button>
        </>
      )}

      <div className="mt-4 flex items-center justify-between text-xs text-slate-400">
        <span>登录即代表同意服务条款与隐私政策</span>
        <Link to={ROUTES.home} className="text-slate-500 hover:text-slate-700">
          返回首页
        </Link>
      </div>
    </Card>
  );
}
