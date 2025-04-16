import {
    IconCheck,
    IconCodeBlock,
    IconDriveFile,
    IconFile,
    IconHome,
    IconSend,
    IconUser,
} from '@arco-design/web-react/icon';
import React from 'react';
import {FaRegHandPointUp} from 'react-icons/fa';
import {IoGitBranchOutline} from 'react-icons/io5';
import {VscDebugBreakpointLog, VscSymbolVariable} from 'react-icons/vsc';
import {GrDirections} from 'react-icons/gr';
import {MdOutlineFunctions} from 'react-icons/md';
import {BsBookmarkCheck, BsBookmarkX} from 'react-icons/bs';
import {TbMathFunction} from 'react-icons/tb';
import Openai from "@/assets/openai_icon.svg";

export const nodeIconsMap = {
    break: <VscDebugBreakpointLog/>,
    condition: <VscDebugBreakpointLog/>,
    confirm: <IconCheck/>,
    field: <IconCodeBlock/>,
    flow: <IconHome/>,
    form: <IconFile/>,
    user: <IconUser/>,
    interrupt: <IoGitBranchOutline/>,
    option: <FaRegHandPointUp/>,
    bot: <IconSend/>,
    rhetorical: <IconSend/>,
    slots: <VscSymbolVariable/>,
    goto: <GrDirections/>,
    'form-gpt': <IconDriveFile/>,
    'functions-gpt': <MdOutlineFunctions/>,
    'function-gpt': <TbMathFunction/>,
    'complete-gpt': <BsBookmarkCheck/>,
    'abort-gpt': <BsBookmarkX/>,
    'slots-gpt': <VscSymbolVariable/>,
    'slot-gpt': <IconCodeBlock/>,
    gpt: <Openai className="icon-size"/>,
};
