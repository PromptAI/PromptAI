import React from 'react';
import {encodeChatUrl} from '@/utils/chatsdk';
import MarkdownPage from "@/pages/entry/templates/components/MarkdownPage";

const TemplateView = ({data}) => {
    const markdown = data.introduction;
    const srcUrl = encodeChatUrl(
        {
            id: data.publishedProjectId,
            token: data.publishedProjectToken,
            project: data.projectId,
            name: 'PromptAI',
            survey: true,
            scene: 'publish_snapshot',
        },
        {
            theme: 'linear-sky',
            'auto-open': true,
            "hide-bot-name": true
        }
    );

    return (
        <>
            <div
                className="flex justify-center">
                <div
                    style={{height: '650px', width: '850px'}}
                    className="relative h-[32rem]  border-natural-200 mr-2 p-2  border-dashed border-2 rounded-lg overflow-auto no-scrollbar col-span-3"
                >
                    <MarkdownPage markdown={markdown}/>
                </div>

                <iframe
                    style={{height: '650px', width: '450px'}}
                    className="border-none  h-full w-full rounded-lg  col-span-2"
                    src={srcUrl}
                    // src={"http://localhost:3000/ava/?config=ZW5naW5lPXJhc2EmaWQ9YTFfcF9kZ3ppY2lveTFubmsmdG9rZW49TkdFNVpERTVNekl0TW1aa05DMDBOemd4TFdJNE1qSXROR1JtTkdSbVlqRTNPR00wJnByb2plY3Q9cF9kZ3ppY2lveTFubmsmc2NlbmU9cHVibGlzaF9kYiZuYW1lPVByb21wdCUyMEFJ" + '&theme=linear-sky&auto-open=true&hide-bot-name=true'}
                />
            </div>
        </>
    );
};

export default TemplateView;
